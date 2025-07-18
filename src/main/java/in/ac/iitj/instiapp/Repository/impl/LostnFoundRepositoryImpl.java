package in.ac.iitj.instiapp.Repository.impl;

import in.ac.iitj.instiapp.Repository.UserRepository;
import in.ac.iitj.instiapp.database.entities.LostnFound.Locations;
import in.ac.iitj.instiapp.database.entities.LostnFound.LostnFoundType;
import in.ac.iitj.instiapp.database.entities.User.Organisation.Organisation;
import in.ac.iitj.instiapp.payload.LostnFound.LostnFoundDto;
import in.ac.iitj.instiapp.payload.User.Student.StudentBaseDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import in.ac.iitj.instiapp.database.entities.LostnFound.LostnFound;
import in.ac.iitj.instiapp.database.entities.User.User;
import in.ac.iitj.instiapp.database.entities.Media.Media;

import java.util.List;
import java.util.Optional;

@Repository
public class LostnFoundRepositoryImpl implements in.ac.iitj.instiapp.Repository.LostnFoundRepository {

    private final JdbcTemplate jdbcTemplate;
    private final EntityManager entityManager;
    private final UserRepository userRepository;


    @Autowired
    public LostnFoundRepositoryImpl(JdbcTemplate jdbcTemplate, EntityManager entityManager, UserRepository userRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.entityManager = entityManager;
        this.userRepository = userRepository;
    }


    @Override
    public Long existLocation(String locationName) {
        return jdbcTemplate.queryForObject("select coalesce(max(id), -1) from locations where name = ?", Long.class, locationName);
    }

    @Override
    public void saveLocation(Locations locations) {
        if(existLocation(locations.getName()) != -1L){
            throw new DataIntegrityViolationException("location already exists with name " + locations.getName());
        }

        this.entityManager.persist(locations);

    }

    @Override
    public List<String> getListOfLocationsName(Pageable pageable) {
        return entityManager.createQuery("select lo.name from Locations lo",String.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
    }

    @Override
    public void updateLocation(String oldLocationName, Locations locations) {
        if(existLocation(oldLocationName) == -1L){
            throw new EmptyResultDataAccessException("old location does not exist with name " + oldLocationName,1);
        }
        else if(existLocation(locations.getName()) != -1L){
            throw new DataIntegrityViolationException("location already exists with name " + locations.getName());
        }
        else{
            String sql = "update locations set name = ? where name = ?";
            jdbcTemplate.update(sql, locations.getName(), oldLocationName);
        }

    }

    @Override
    @Transactional
    public void deleteLocationByName(String locationName) {
        if(existLocation(locationName) == -1){
            throw new DataIntegrityViolationException("location doesnt exist with name " + locationName);
        }
        else {
            // step 1 :-first set the locations null in lost and found if present
            entityManager.createQuery(
                    "UPDATE LostnFound lf SET lf.Landmark = null WHERE lf.Landmark.name = :name"
            ).setParameter("name", locationName).executeUpdate();

            // Step 2: Delete the Location
            entityManager.createQuery(
                    "DELETE FROM Locations l WHERE l.name = :name"
            ).setParameter("name", locationName).executeUpdate();
        }
    }

    @Override
    public Long exsitLostnFound(String publicId) {
        return jdbcTemplate.queryForObject("select coalesce(max(id), -1) from lostnfound where public_id= ?", Long.class, publicId);
    }

    @Override
    public void saveLostnFoundDetails(LostnFound lostnFound){
        entityManager.persist(lostnFound);
    }

    @Override
    public List<LostnFoundDto> getLostnFoundByFilter(
            LostnFoundType type,
            Optional<Boolean> status,
            Optional<String> owner,
            Optional<String> finder,
            Optional<String> landmark,
            Pageable pageable) {

        String ql =
                "select new in.ac.iitj.instiapp.payload.LostnFound.LostnFoundDto(" +
                        "l.publicId, " +
                        "f.userName, " +            // alias for finder
                        "o.userName, " +            // alias for owner
                        "lm.name, " +               // alias for landmark
                        "l.type, " +
                        "l.extraInfo, " +
                        "l.status, " +
                        "m.publicId" +              // alias for media
                        ") " +
                        "from LostnFound l " +
                        "left join l.finder f " +
                        "left join l.owner o " +
                        "left join l.Landmark lm " +   // or l.landmark if your field is lowercase
                        "left join l.media m " +
                        "where l.type = :type " +
                        "AND (:status is null or l.status = :status) " +
                        "AND (:owner is null or o.userName = :owner) " +
                        "AND (:finder is null or f.userName = :finder) " +
                        "AND (:landmark is null or lm.name = :landmark)";

        TypedQuery<LostnFoundDto> query = entityManager.createQuery(ql, LostnFoundDto.class);
        query.setParameter("type", type);
        query.setParameter("status", status.orElse(null));
        query.setParameter("owner", owner.orElse(null));
        query.setParameter("finder", finder.orElse(null));
        query.setParameter("landmark", landmark.orElse(null));

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return query.getResultList();
    }



    @Override
    public void updateLostnFound(LostnFound lostnFound, String publicId) {
        if(exsitLostnFound(publicId) == -1L){
            throw new EmptyResultDataAccessException("the lost and found with public id " + publicId +"does not exists" , 1);
        }
        else {
            jdbcTemplate.update(
                    "UPDATE lostnfound SET " +
                            "finder_id = ?, " +
                            "owner_id = ?, " +
                            "landmark_id = CASE WHEN cast(? as integer) IS NULL THEN landmark_id ELSE ? END, " +
                            "extra_info = CASE WHEN ? IS NULL THEN extra_info ELSE ? END, " +
                            "status = CASE WHEN ? IS NULL THEN status ELSE ? END, " +
                            "media_id = CASE WHEN cast(? as integer) IS NULL THEN media_id ELSE ? END " +
                            "WHERE public_id = ?",
                    // Parameters for query in order:
                    Optional.ofNullable(lostnFound.getFinder())
                            .map(User::getId).orElse(null),
                    Optional.ofNullable(lostnFound.getOwner())
                            .map(User::getId).orElse(null),

                    Optional.ofNullable(lostnFound.getLandmark())
                            .map(Locations::getId).orElse(null),
                    Optional.ofNullable(lostnFound.getLandmark())
                            .map(Locations::getId).orElse(null),

                    lostnFound.getExtraInfo(),
                    lostnFound.getExtraInfo(),

                    lostnFound.getStatus(),
                    lostnFound.getStatus(),

                    Optional.ofNullable(lostnFound.getMedia())
                            .map(Media::getId).orElse(null),
                    Optional.ofNullable(lostnFound.getMedia())
                            .map(Media::getId).orElse(null),

                    publicId
            );
        }
    }

    @Override
    public Optional<String> deleteLostnFound(String publicId) {
        String sql = """
            SELECT m.public_id
            FROM media m
            JOIN lostnfound l ON m.id = l.media_id
            WHERE l.public_id = ?
        """;
        List<String> mediaPublicIds = jdbcTemplate.queryForList(sql, String.class, publicId);
        String mediaPublicId = mediaPublicIds.isEmpty() ? null : mediaPublicIds.get(0);

        String deleteSql = """
                DELETE FROM lostnfound
                WHERE public_id = ?
            """;
        int rowsAffected = jdbcTemplate.update(deleteSql, publicId);

        if (rowsAffected == 0) {
            throw new EmptyResultDataAccessException("No lost and found item with public id " + publicId, 1);
        }

        return Optional.ofNullable(mediaPublicId);

    }

    public boolean isOwner(String userName, String publicId){
        TypedQuery<String> query = entityManager.createQuery("SELECT l.owner.userName FROM LostnFound l WHERE l.publicId = :publicId", String.class)
                                                .setParameter("publicId", publicId);
        return query.getSingleResult().equals(userName);
    }
    public boolean isFinder(String userName, String publicId){
        TypedQuery<String> query = entityManager.createQuery("SELECT l.finder.userName FROM LostnFound l WHERE l.publicId = :publicId", String.class)
                .setParameter("publicId", publicId);
        return query.getSingleResult().equals(userName);
    }

    public LostnFoundType findTypeByPublicId(String publicId){
        TypedQuery<LostnFoundType> Query = entityManager.createQuery("SELECT l.type FROM LostnFound l WHERE l.publicId = :publicId", LostnFoundType.class)
                .setParameter("publicId", publicId);
        return Query.getSingleResult();
    }


}





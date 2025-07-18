package in.ac.iitj.instiapp.Tests.Repository;

import in.ac.iitj.instiapp.Repository.MessRepository;
import in.ac.iitj.instiapp.Repository.impl.MessRepositoryImpl;
import in.ac.iitj.instiapp.database.entities.Scheduling.MessMenu.*;
import in.ac.iitj.instiapp.payload.Scheduling.MessMenu.MenuOverrideDto;
import in.ac.iitj.instiapp.payload.Scheduling.MessMenu.MessMenuDto;
import jakarta.persistence.NoResultException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;

import java.text.ParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static in.ac.iitj.instiapp.Tests.EntityTestData.MenuData.*;
import static in.ac.iitj.instiapp.Tests.EntityTestData.OverrideMenudata.MESS_OVERRIDE1;
import static in.ac.iitj.instiapp.Tests.EntityTestData.OverrideMenudata.MESS_OVERRIDE2;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({MessRepositoryImpl.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) //  Cannot be removed as the repository uses postgres specific schema for queries
public class MenuTest {

    @Autowired
    MessRepository messRepository;

    @BeforeAll
    public static void setUp(@Autowired MessRepository messRepository) throws ParseException {


        messRepository.saveMessMenu(MENU1.toEntity());
        messRepository.saveMessMenu(MENU2.toEntity());

        MenuOverride menuoverride1 = MESS_OVERRIDE1.toEntity();

        messRepository.saveOverrideMessMenu(menuoverride1);
    }

    @Test
    @Order(1)
    public void testMessMenuExists() {
        Assertions.assertThat(messRepository.messMenuExists(MENU1.year, MENU1.month, MENU1.day)).isTrue();
        Assertions.assertThat(messRepository.messMenuExists(MENU3.year, MENU3.month, MENU3.day)).isFalse();
    }

    @Test
    @Order(2)
    public void testMenuOverrideExists() throws ParseException {

        Assertions.assertThat(messRepository.menuOverrideExists(MESS_OVERRIDE1.date)).isTrue();
        Assertions.assertThat(messRepository.menuOverrideExists(MESS_OVERRIDE2.date)).isFalse();
    }

    @Test
    @Order(3)
    public void testGetMessMenu() {

        int year = 2024;
        int month = 6;

        List<MessMenuDto> result = messRepository.getMessMenu(year, month);

        assertEquals(2, result.size(), "The number of menus returned should match the mock data");

        Assertions.assertThat(result)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("Id")
                .containsExactlyInAnyOrder(MENU1.messMenuDto(),MENU2.messMenuDto());



    }

    @Test
    @Order(4)
    public void testGetMenuOverride() throws ParseException {


        MenuOverrideDto result = messRepository.getOverrideMessMenu(MESS_OVERRIDE1.date);

        assertEquals(MESS_OVERRIDE1.menuItemData.toEntity().getBreakfast(), result.getMenuItemBreakfast());
        assertEquals(MESS_OVERRIDE1.menuItemData.toEntity().getLunch(), result.getMenuItemLunch());
        assertEquals(MESS_OVERRIDE1.menuItemData.toEntity().getSnacks(), result.getMenuItemSnacks());
        assertEquals(MESS_OVERRIDE1.menuItemData.toEntity().getDinner(), result.getMenuItemDinner());
    }

    @Test
    @Order(5)
    @Rollback(value = true)
    public void testDeleteMessMenu() {

        boolean existsBefore = messRepository.messMenuExists(MENU1.year, MENU1.month, MENU1.day);
        assertTrue(existsBefore, "The record should exist before deletion");

        // Act: Call the delete method
        messRepository.deleteMessMenu(MENU1.year, MENU1.month, MENU1.day);

        // Assert: Verify that the record no longer exists after deletion
        boolean existsAfter = messRepository.messMenuExists(MENU1.year, MENU1.month, MENU1.day);
        assertFalse(existsAfter, "The record should be deleted from the table");

    }

    @Test
    @Order(6)
    @Rollback(value = true)
    public void testUpdateMenuOverride() throws ParseException {
        boolean existsBefore = messRepository.menuOverrideExists(MESS_OVERRIDE1.date);
        assertTrue(existsBefore, "The record should exist before updating");
        messRepository.updateOverrideMessMenu(MESS_OVERRIDE2.menuItemData.toEntity(), MESS_OVERRIDE1.date);
        MenuOverrideDto newmenu = messRepository.getOverrideMessMenu(MESS_OVERRIDE1.date);
        assertEquals(MESS_OVERRIDE2.menuItemData.toEntity().getBreakfast(), newmenu.getMenuItemBreakfast());
        assertEquals(MESS_OVERRIDE2.menuItemData.toEntity().getLunch(), newmenu.getMenuItemLunch());
        assertEquals(MESS_OVERRIDE2.menuItemData.toEntity().getSnacks(), newmenu.getMenuItemSnacks());
        assertEquals(MESS_OVERRIDE2.menuItemData.toEntity().getDinner(), newmenu.getMenuItemDinner());
    }

    @Test
    @Order(7)
    @Rollback(value = true)
    public void testDeleteOverrideMessMenu() throws Exception {


        // Ensure the record exists before deletion
        boolean existsBefore = messRepository.menuOverrideExists(MESS_OVERRIDE1.date);
        assertTrue(existsBefore, "The record should exist before deletion");

        // Act: Call the delete method
        messRepository.deleteOverrideMessMenu(MESS_OVERRIDE1.date);

        // Assert: Verify that the record no longer exists after deletion
        boolean existsAfter = messRepository.menuOverrideExists(MESS_OVERRIDE1.date);
        assertFalse(existsAfter, "The record should be deleted from the table");
    }


    @Test
    @Order(8)
    @Rollback(value = true)
    public void testUpdateMessMenu() {
        boolean existsBefore = messRepository.messMenuExists(MENU2.year, MENU2.month, MENU2.day);  // Assuming 6th December 2024
        assertTrue(existsBefore, "The record should exist before updating");

        messRepository.updateMessMenu(MENU2.year, MENU2.month, MENU2.day, MENU3.menuItemData.toEntity());

        List<MessMenuDto> updatedMenu = messRepository.getMessMenu(2024, 6);
        assertNotNull(updatedMenu, "The updated menu should not be null");

        Assertions.assertThat(updatedMenu.get(1).getMenuItemBreakfast())
                .isEqualTo(MENU3.menuItemData.toEntity().getBreakfast());
        Assertions.assertThat(updatedMenu.get(1).getMenuItemLunch())
                .isEqualTo(MENU3.menuItemData.toEntity().getLunch());
        Assertions.assertThat(updatedMenu.get(1).getMenuItemSnacks())
                .isEqualTo(MENU3.menuItemData.toEntity().getSnacks());
        Assertions.assertThat(updatedMenu.get(1).getMenuItemDinner())
                .isEqualTo(MENU3.menuItemData.toEntity().getDinner());


    }


}

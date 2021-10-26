package teletubbies.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static teletubbies.testutil.TypicalPersons.AMY;
import static teletubbies.testutil.TypicalPersons.BOB;
import static teletubbies.testutil.TypicalPersons.CARL;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import teletubbies.commons.exceptions.EarliestVersionException;
import teletubbies.commons.exceptions.LatestVersionException;
import teletubbies.model.VersionedAddressBook.EmptyAddressBookStateListException;
import teletubbies.testutil.AddressBookBuilder;

public class VersionedAddressBookTest {
    private final ReadOnlyAddressBook addressBookWithAmy = new AddressBookBuilder().withPerson(AMY).build();
    private final ReadOnlyAddressBook addressBookWithBob = new AddressBookBuilder().withPerson(BOB).build();
    private final ReadOnlyAddressBook addressBookWithCarl = new AddressBookBuilder().withPerson(CARL).build();
    private final ReadOnlyAddressBook emptyAddressBook = new AddressBookBuilder().build();
    private final ReadOnlyAddressBook addressBookWithCarlAndAmy =
            new AddressBookBuilder().withPerson(CARL).withPerson(AMY).build();
    private final ReadOnlyAddressBook addressBookWithAmyAndBob =
            new AddressBookBuilder().withPerson(AMY).withPerson(BOB).build();
    private final ReadOnlyAddressBook addressBookWithCarlAndBob =
            new AddressBookBuilder().withPerson(CARL).withPerson(BOB).build();
    private final ReadOnlyAddressBook addressBookWithAmyBobAndCarl =
            new AddressBookBuilder().withPerson(AMY).withPerson(BOB).withPerson(CARL).build();
    private final List<ReadOnlyAddressBook> fullStateList =
            List.of(emptyAddressBook, addressBookWithAmy, addressBookWithBob, addressBookWithCarl,
                    addressBookWithCarlAndAmy, addressBookWithAmyAndBob, addressBookWithCarlAndBob,
                    addressBookWithAmyBobAndCarl, emptyAddressBook);

    @Test
    public void commit_oneAddressBook_noStatesRemoved()
            throws LatestVersionException, EarliestVersionException, EmptyAddressBookStateListException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(emptyAddressBook);
        versionedAddressBook.commit();
        assertAddressBookListStatus(versionedAddressBook,
                Collections.singletonList(emptyAddressBook),
                emptyAddressBook,
                Collections.emptyList());
    }

    @Test
    public void commit_multipleAddressBooks_noStatesRemoved()
            throws EmptyAddressBookStateListException, LatestVersionException, EarliestVersionException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy, addressBookWithBob);

        versionedAddressBook.commit();
        assertAddressBookListStatus(versionedAddressBook,
                List.of(emptyAddressBook, addressBookWithAmy, addressBookWithBob),
                addressBookWithBob,
                List.of());
    }

    /**
     * Tests {@code VersionedAddressBookTest#commit_multipleAddressBooks_randomNumberOfStatesRemoved} multiple times.
     */
    @Test
    public void testMultipleCasesOfRandomStateRemoval()
            throws LatestVersionException, EmptyAddressBookStateListException, EarliestVersionException {
        for (int i = 0; i < 10; i++) {
            commit_multipleAddressBooks_randomNumberOfStatesRemoved();
        }
    }

    /**
     * Generates a random number of times to undo and checks that when the VersionedAddressBook is committed,
     * rest of the states are removed, and it contains the relevant states.
     */
    private void commit_multipleAddressBooks_randomNumberOfStatesRemoved()
            throws EmptyAddressBookStateListException, EarliestVersionException, LatestVersionException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(fullStateList);
        int random = generateRandomNumber(0, fullStateList.size());
        List<ReadOnlyAddressBook> fullList =
                List.copyOf(fullStateList);
        undoVersionedAddressBookNTimes(versionedAddressBook, random);
        versionedAddressBook.commit();
        assertAddressBookListStatus(versionedAddressBook,
                fullList.subList(0, fullStateList.size() - random),
                fullList.get(fullStateList.size() - 1 - random),
                List.of());
    }

    @Test
    public void canUndo_multiple_returnsTrue() throws EmptyAddressBookStateListException, EarliestVersionException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy, addressBookWithBob);
        assertTrue(versionedAddressBook.canUndo());
        versionedAddressBook.undo();
        assertTrue(versionedAddressBook.canUndo());
    }

    @Test
    public void canUndo_pointingToStart_returnsFalse()
            throws EmptyAddressBookStateListException, EarliestVersionException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy, addressBookWithBob);
        undoVersionedAddressBookNTimes(versionedAddressBook, 2);
        assertFalse(versionedAddressBook.canUndo());
    }

    @Test
    public void canUndo_single_returnsFalse() throws EmptyAddressBookStateListException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(emptyAddressBook);
        assertFalse(versionedAddressBook.canUndo());
    }

    @Test
    public void canRedo_single_returnsFalse() throws EmptyAddressBookStateListException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(emptyAddressBook);
        assertFalse(versionedAddressBook.canRedo());
    }

    @Test
    public void canRedo_multiplePointingToStart_returnsFalse()
            throws EmptyAddressBookStateListException, EarliestVersionException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy, addressBookWithBob);
        undoVersionedAddressBookNTimes(versionedAddressBook, 2);
        assertTrue(versionedAddressBook.canRedo());
    }

    @Test
    public void undo_multipleAtEndOfStateList_success()
            throws EmptyAddressBookStateListException, EarliestVersionException, LatestVersionException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy, addressBookWithBob);

        versionedAddressBook.undo();
        assertAddressBookListStatus(versionedAddressBook,
                List.of(emptyAddressBook),
                addressBookWithAmy,
                List.of(addressBookWithBob));
    }

    @Test
    public void undo_multipleNotAtEndOfStateList_success()
            throws EmptyAddressBookStateListException, EarliestVersionException, LatestVersionException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy, addressBookWithBob);
        undoVersionedAddressBookNTimes(versionedAddressBook, 1);

        versionedAddressBook.undo();
        assertAddressBookListStatus(versionedAddressBook,
                List.of(),
                emptyAddressBook,
                List.of(addressBookWithAmy, addressBookWithBob));
    }

    @Test
    public void undo_single_throwsEarliestVersionException()
            throws EmptyAddressBookStateListException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(emptyAddressBook);

        assertThrows(EarliestVersionException.class, versionedAddressBook::undo);
    }

    @Test
    public void undo_multiple_throwsEarliestVersionException()
            throws EmptyAddressBookStateListException, EarliestVersionException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy, addressBookWithBob);
        undoVersionedAddressBookNTimes(versionedAddressBook, 2);
        assertThrows(EarliestVersionException.class, versionedAddressBook::undo);
    }

    @Test
    public void redo_multipleAtEndOfStateList_success()
            throws EarliestVersionException, EmptyAddressBookStateListException, LatestVersionException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy, addressBookWithBob);
        undoVersionedAddressBookNTimes(versionedAddressBook, 1);

        versionedAddressBook.redo();
        assertAddressBookListStatus(versionedAddressBook,
                List.of(emptyAddressBook, addressBookWithAmy),
                addressBookWithBob,
                List.of());
    }

    @Test
    public void redo_multipleAtStartOfStateList_returnsTrue()
            throws LatestVersionException, EarliestVersionException, EmptyAddressBookStateListException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy, addressBookWithBob);
        undoVersionedAddressBookNTimes(versionedAddressBook, 2);

        versionedAddressBook.redo();
        assertAddressBookListStatus(versionedAddressBook,
                List.of(emptyAddressBook),
                addressBookWithAmy,
                List.of(addressBookWithBob));
    }

    @Test
    public void redo_singleAddressBook_throwsLatestVersionException() throws EmptyAddressBookStateListException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(emptyAddressBook);

        assertThrows(LatestVersionException.class, versionedAddressBook::redo);
    }

    @Test
    public void equalsTest() throws EmptyAddressBookStateListException, EarliestVersionException {
        VersionedAddressBook versionedAddressBook = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy);

        VersionedAddressBook copy = prepareVersionedAddressBook(
                emptyAddressBook, addressBookWithAmy);

        assertEquals(versionedAddressBook, copy);

        assertEquals(versionedAddressBook, versionedAddressBook);

        assertNotEquals(null, versionedAddressBook);

        VersionedAddressBook differentAddressBookList = prepareVersionedAddressBook(
                addressBookWithBob, addressBookWithCarl);

        assertNotEquals(differentAddressBookList, versionedAddressBook);

        undoVersionedAddressBookNTimes(copy, 1);
        assertNotEquals(copy, versionedAddressBook);

    }


    /**
     * Verifies that {@code versionedAddressBook} is currently pointing at {@code expectedCurrentState},
     * states before current state is equal to {@code expectedStatesBeforePointer},
     * and states after current state is equal to {@code expectedStatesAfterPointer}.
     */
    private void assertAddressBookListStatus(VersionedAddressBook versionedAddressBook,
                                             List<ReadOnlyAddressBook> expectedStatesBeforeCurrentState,
                                             ReadOnlyAddressBook expectedCurrentState,
                                             List<ReadOnlyAddressBook> expectedStatesAfterCurrentState)
            throws EarliestVersionException, LatestVersionException {

        // check if current state is correct
        assertEquals(new AddressBook(versionedAddressBook), expectedCurrentState);

        // revert to the earliest version of versionedAddressBook
        while (versionedAddressBook.canUndo()) {
            versionedAddressBook.undo();
        }

        // verify states before current state
        for (ReadOnlyAddressBook expectedAddressBook : expectedStatesBeforeCurrentState) {
            assertEquals(expectedAddressBook, new AddressBook(versionedAddressBook));
            versionedAddressBook.redo();
        }

        // verify states after currentState
        for (ReadOnlyAddressBook expectedAddressBook : expectedStatesAfterCurrentState) {
            versionedAddressBook.redo();
            assertEquals(expectedAddressBook, new AddressBook(versionedAddressBook));
        }

        // check that there are no more states after pointer
        assertFalse(versionedAddressBook.canRedo());

        // revert pointer to original position
        for (int i = 0; i < expectedStatesAfterCurrentState.size(); i++) {
            versionedAddressBook.undo();
        }
    }

    /**
     * Creates a loaded {@code VersionedAddressBook} with the relevant states.
     */
    private VersionedAddressBook prepareVersionedAddressBook(ReadOnlyAddressBook... addressBooks)
            throws EmptyAddressBookStateListException {
        assertTrue(addressBooks.length != 0);
        return VersionedAddressBook.of(addressBooks);
    }

    /**
     * Creates a loaded {@code VersionedAddressBook} with the relevant states.
     */
    private VersionedAddressBook prepareVersionedAddressBook(List<ReadOnlyAddressBook> addressBooks)
            throws EmptyAddressBookStateListException {
        assertFalse(addressBooks.isEmpty());
        return VersionedAddressBook.of(addressBooks);
    }

    private void undoVersionedAddressBookNTimes(VersionedAddressBook versionedAddressBook, int n)
            throws EarliestVersionException {
        for (int i = 0; i < n; i++) {
            versionedAddressBook.undo();
        }
    }

    /**
     * Generates random number between the range [lowest, highest).
     */
    private int generateRandomNumber(int lowest, int highest) {
        return lowest + (int) (Math.random() * (highest - lowest));
    }

}

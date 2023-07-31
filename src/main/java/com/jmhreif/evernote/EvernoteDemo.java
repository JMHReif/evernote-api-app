package com.jmhreif.evernote;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.clients.UserStoreClient;
import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Notebook;
import com.evernote.thrift.transport.TTransportException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EvernoteDemo implements CommandLineRunner {
    @Value("${AUTH_TOKEN}")
    private String token;

    private UserStoreClient userStore;
    private NoteStoreClient noteStore;

    @Override
    public void run(String... args) throws Exception {
        if ("your developer token".equals(token)) {
            System.err.println("Please fill in your developer token");
            System.err
                    .println("To get a developer token, go to https://sandbox.evernote.com/api/DeveloperToken.action");
            return;
        }

        EvernoteDemo demo = new EvernoteDemo(token);
        try {
            demo.listNotes();
        } catch (EDAMUserException e) {
            // These are the most common error types that you'll need to handle
            // EDAMUserException is thrown when an API call fails because a
            // parameter was invalid.
            if (e.getErrorCode() == EDAMErrorCode.AUTH_EXPIRED) {
                System.err.println("Your authentication token is expired!");
            } else if (e.getErrorCode() == EDAMErrorCode.INVALID_AUTH) {
                System.err.println("Your authentication token is invalid!");
            } else if (e.getErrorCode() == EDAMErrorCode.QUOTA_REACHED) {
                System.err.println("Your authentication token is invalid!");
            } else {
                System.err.println("Error: " + e.getErrorCode().toString()
                        + " parameter: " + e.getParameter());
            }
        } catch (EDAMSystemException e) {
            System.err.println("System error: " + e.getErrorCode().toString());
        } catch (TTransportException t) {
            System.err.println("Networking error: " + t.getMessage());
        }
    }

    public EvernoteDemo() {}

    /**
     * Intialize UserStore and NoteStore clients. During this step, we
     * authenticate with the Evernote web service. All of this code is boilerplate
     * - you can copy it straight into your application.
     */
    public EvernoteDemo(String token) throws Exception {
        // Set up the UserStore client and check that we can speak to the server
        EvernoteAuth evernoteAuth = new EvernoteAuth(EvernoteService.SANDBOX, token);
        ClientFactory factory = new ClientFactory(evernoteAuth);
        userStore = factory.createUserStoreClient();

        boolean versionOk = userStore.checkVersion("Evernote EDAMDemo (Java)",
                com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR,
                com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
        if (!versionOk) {
            System.err.println("Incompatible Evernote client protocol version");
            System.exit(1);
        }

        // Set up the NoteStore client
        noteStore = factory.createNoteStoreClient();
    }

    /**
     * Retrieve and display a list of the user's notes.
     */
    private void listNotes() throws Exception {
        // List the notes in the user's account
        System.out.println("Listing notes:");

        // First, get a list of all notebooks
        List<Notebook> notebooks = noteStore.listNotebooks();

        for (Notebook notebook : notebooks) {
            System.out.println("Notebook: " + notebook.getName());

            // Next, search for the first 100 notes in this notebook, ordering
            // by creation date
            NoteFilter filter = new NoteFilter();
            filter.setNotebookGuid(notebook.getGuid());
            filter.setOrder(NoteSortOrder.CREATED.getValue());
            filter.setAscending(true);

            NoteList noteList = noteStore.findNotes(filter, 0, 100);
            List<Note> notes = noteList.getNotes();
            for (Note note : notes) {
                System.out.println(" * " + note.getTitle());
            }
        }
        System.out.println();
    }
}

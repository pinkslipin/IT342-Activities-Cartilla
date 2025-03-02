package com.craigmatthew.oauth2login.service;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.*;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Service
public class GoogleContactsService {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public GoogleContactsService(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    private PeopleService getPeopleService(OAuth2AuthenticationToken authentication) throws IOException {
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );

        if (authorizedClient == null || authorizedClient.getAccessToken() == null) {
            throw new IOException("No OAuth2 access token found.");
        }

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        // ✅ Use full access scope to allow add, edit, and delete
        GoogleCredentials credentials = GoogleCredentials.create(new AccessToken(accessToken, null))
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/contacts"));

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        return new PeopleService.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), requestInitializer)
                .setApplicationName("oauth2login")
                .build();
    }

    // ✅ Fetch Google Contacts
    public List<Person> getContacts(OAuth2AuthenticationToken authentication) throws IOException {
        PeopleService peopleService = getPeopleService(authentication);

        ListConnectionsResponse response = peopleService.people().connections()
                .list("people/me")
                .setPersonFields("names,emailAddresses,phoneNumbers")
                .execute();

        return response.getConnections() != null ? response.getConnections() : Collections.emptyList();
    }

    // ✅ Create a new Google Contact
    public Person createContact(OAuth2AuthenticationToken authentication, String name, String email) throws IOException {
        PeopleService peopleService = getPeopleService(authentication);

        Person person = new Person()
                .setNames(Collections.singletonList(new Name().setGivenName(name)))
                .setEmailAddresses(Collections.singletonList(new EmailAddress().setValue(email)));

        return peopleService.people().createContact(person).execute();
    }

    // ✅ Update an existing contact (Fix: Include etag)
    public Person updateContact(OAuth2AuthenticationToken authentication, String resourceName, String newName, String newEmail) throws IOException {
        PeopleService peopleService = getPeopleService(authentication);

        // 🔹 Fetch the existing contact to get the etag
        Person existingContact = peopleService.people().get(resourceName)
                .setPersonFields("names,emailAddresses")
                .execute();

        if (existingContact == null) {
            throw new IOException("Contact not found: " + resourceName);
        }

        Person updatedPerson = new Person()
                .setEtag(existingContact.getEtag())  // Required to prevent conflicts
                .setNames(Collections.singletonList(new Name().setGivenName(newName)))
                .setEmailAddresses(Collections.singletonList(new EmailAddress().setValue(newEmail)));

        return peopleService.people().updateContact(resourceName, updatedPerson)
                .setUpdatePersonFields("names,emailAddresses")
                .execute();
    }

    // ✅ Delete a Google Contact
    public void deleteContact(OAuth2AuthenticationToken authentication, String resourceName) throws IOException {
        PeopleService peopleService = getPeopleService(authentication);
        peopleService.people().deleteContact(resourceName).execute();
    }
}

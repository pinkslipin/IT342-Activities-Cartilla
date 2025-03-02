package com.craigmatthew.oauth2login.controller;

import com.craigmatthew.oauth2login.service.GoogleContactsService;
import com.google.api.services.people.v1.model.Person;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/googlecontacts")
public class GoogleContactsController {

    private final GoogleContactsService googleContactsService;

    public GoogleContactsController(GoogleContactsService googleContactsService) {
        this.googleContactsService = googleContactsService;
    }

    @GetMapping
    public List<Person> getContacts(OAuth2AuthenticationToken authentication) throws IOException {
        return googleContactsService.getContacts(authentication);
    }

    @PostMapping
    public Person createContact(OAuth2AuthenticationToken authentication,
                                @RequestParam String name,
                                @RequestParam String email) throws IOException {
        return googleContactsService.createContact(authentication, name, email);
    }

    @PutMapping("/{resourceName}")
    public Person updateContact(OAuth2AuthenticationToken authentication,
                                @PathVariable String resourceName,
                                @RequestParam String newName,
                                @RequestParam String newEmail) throws IOException {
        return googleContactsService.updateContact(authentication, resourceName, newName, newEmail);
    }

    @DeleteMapping("/{resourceName}")
    public void deleteContact(OAuth2AuthenticationToken authentication,
                              @PathVariable String resourceName) throws IOException {
        googleContactsService.deleteContact(authentication, resourceName);
    }
}

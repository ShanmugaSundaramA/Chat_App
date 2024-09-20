package com.ws.chat.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "users")
public class User implements UserDetails {

     private static final long serialVersionUID = 1L;

     @Id
     private String id;
     private String userId;
     private String fullname;
     private String email;
     private String designation;
     private String department;
     private String mobileNo;
     private String colorCode;
     private Date lastseen;
     private boolean isOnline;
     private Setting setting;
     private String profilePicture;
     private String deviceToken;
     private transient List<PinnedRecipient> pinnedRecipientsId;
     private List<String> mutedRecipientIds;
     @CreatedDate
     private Date createdAt;
     @LastModifiedDate
     private Date updatedAt;
     private String role;
     @JsonIgnore
     private String password;
     @JsonIgnore
     private String authorities;
     @JsonIgnore
     private String enabled;
     @JsonIgnore
     private String username;
     @JsonIgnore
     private String credentialsNonExpired;
     @JsonIgnore
     private String accountNonExpired;
     @JsonIgnore
     private String accountNonLocked;

     @Override
     public Collection<? extends GrantedAuthority> getAuthorities() {
          return new ArrayList<>();
     }

     @Override
     public String getUsername() { // NOSONAR
          return email;
     }

     @Override
     public boolean isAccountNonExpired() {
          return true;
     }

     @Override
     public boolean isAccountNonLocked() {
          return true;
     }

     @Override
     public boolean isCredentialsNonExpired() {
          return true;
     }

     @Override
     public boolean isEnabled() {
          return true;
     }

     @Override
     public String getPassword() {
          return null;
     }
}

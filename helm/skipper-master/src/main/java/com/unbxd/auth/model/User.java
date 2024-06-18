package com.unbxd.auth.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class User extends UserCred {

    Regions regions;

    public User(String email, String password) {
        super(email, password);
    }

    public User(String email, String password, Regions regions) {
        super(email, password);
        this.regions = regions;
    }

    private Boolean getActivate() {
        return Boolean.TRUE;
    }
}


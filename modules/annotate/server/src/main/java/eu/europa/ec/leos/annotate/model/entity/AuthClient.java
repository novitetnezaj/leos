/*
 * Copyright 2018 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.annotate.model.entity;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "AUTHCLIENTS", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"SECRET"}),
        @UniqueConstraint(columnNames = {"CLIENT_ID"})
})
public class AuthClient {

    /**
     * Class representing an annotate client trying to authenticate users 
     */

    // -------------------------------------
    // column definitions
    // -------------------------------------

    @Id
    @Column(name = "ID", nullable = false)
    @GenericGenerator(name = "authclientsSequenceGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "AUTHCLIENTS_SEQ"),
            // @Parameter(name = "initial_value", value = "1000"),
            @Parameter(name = "increment_size", value = "1")
    })
    @GeneratedValue(generator = "authclientsSequenceGenerator")
    private long id;

    // human-readable description
    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    // secret key of the client
    @Column(name = "SECRET", nullable = false, unique = true)
    private String secret;

    // ID of the client; used in "issuer" field of JWT
    @Column(name = "CLIENT_ID", nullable = false, unique = true)
    private String clientId;

    // list of authorities that the client may authenticate; separated by semi-colon
    @Column(name = "AUTHORITIES", nullable = true)
    private String authorities;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------
    public AuthClient() {
        // default constructor required by JPA
    }

    public AuthClient(String description, String secret, String clientId, String authorities) {

        this.description = description;
        this.secret = secret;
        this.clientId = clientId;
        this.authorities = authorities;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getAuthorities() {
        return authorities;
    }

    public void setAuthorities(String authorities) {
        this.authorities = authorities;
    }

    // return a list of the individual authorities for which the client may authenticate
    @Transient
    public List<String> getAuthoritiesList() {

        if (this.authorities == null) {
            return null;
        }
        return Arrays.asList(this.authorities.split(";"));
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Override
    public int hashCode() {
        return Objects.hash(id, description, secret, clientId, authorities);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final AuthClient other = (AuthClient) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.secret, other.secret) &&
                Objects.equals(this.clientId, other.clientId) &&
                Objects.equals(this.authorities, other.authorities);
    }
}

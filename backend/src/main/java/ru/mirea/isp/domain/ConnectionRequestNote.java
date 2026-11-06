package ru.mirea.isp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/** Операторская заметка, привязанная к карточке заявки. */
@Entity
@Table(name = "request_note")
public class ConnectionRequestNote extends AuditedVersionedEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ConnectionRequest request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private boolean pinned;

    protected ConnectionRequestNote() {
    }

    public ConnectionRequestNote(UUID id, ConnectionRequest request, UserAccount author, String body, boolean pinned) {
        this.id = id;
        this.request = request;
        this.author = author;
        this.body = body;
        this.pinned = pinned;
    }

    public UUID getId() { return id; }
    public ConnectionRequest getConnectionRequest() { return request; }
    public UserAccount getAuthor() { return author; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}

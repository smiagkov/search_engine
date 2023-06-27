package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "site")
@Data
@NoArgsConstructor
public class SiteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "site", cascade = CascadeType.REMOVE)
    List<PageEntity> pages = new ArrayList<>();
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "site", cascade = CascadeType.REMOVE)
    List<LemmaEntity> lemmas = new ArrayList<>();
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('INDEXING', 'INDEXED', 'FAILED')", nullable = false)
    private SiteStatus status;
    @Column(name = "status_time", columnDefinition = "DATETIME", nullable = false)
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String url;
    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String name;

    public SiteEntity(String name, String url) {
        this(name, url, SiteStatus.INDEXING);
    }

    public SiteEntity(String name, String url, SiteStatus siteStatus) {
        this.status = siteStatus;
        this.statusTime = LocalDateTime.now();
        this.url = url;
        this.name = name;
    }

    public void update() {
        this.statusTime = LocalDateTime.now();
    }

    public void update(SiteStatus status) {
        this.status = status;
        update();
    }

    public void update(String lastError) {
        this.lastError = lastError;
        update(SiteStatus.FAILED);
    }
}

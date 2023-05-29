package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "page", indexes = @Index(name = "path", columnList = "path"))
@Data
@NoArgsConstructor
public class PageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "page", cascade = CascadeType.REMOVE)
    List<IndexEntity> indexes = new ArrayList<>();
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_PAGE_SITE_ID",
                    foreignKeyDefinition = "FOREIGN KEY (`site_id`) REFERENCES `site`(`id`) ON DELETE CASCADE ON UPDATE NO ACTION"))
    private SiteEntity site;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;



    public PageEntity(SiteEntity site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public PageEntity(SiteEntity site, String path) {
        this(site, path, 0, "");
    }

    public void update(int code, String content) {
        update(code);
        this.content = content;
    }

    public void update(int code) {
        this.code = code;
    }
}

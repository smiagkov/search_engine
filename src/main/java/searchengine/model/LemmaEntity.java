package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lemma", uniqueConstraints = @UniqueConstraint(
        name = "UC_SITE_LEMMA", columnNames = {"site_id", "lemma"}))
@Data
public class LemmaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "lemma", cascade = CascadeType.REMOVE)
    List<IndexEntity> indexes = new ArrayList<>();
    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_LEMMA_SITE_ID",
                    foreignKeyDefinition = "FOREIGN KEY (`site_id`) REFERENCES `site`(`id`) ON DELETE CASCADE ON UPDATE NO ACTION"))
    private SiteEntity site;
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String lemma;
    @Column(nullable = false)
    private int frequency;

    public LemmaEntity() {
    }

    public LemmaEntity(SiteEntity site, String lemma) {
        this.site = site;
        this.lemma = lemma;
        this.frequency = 1;
    }
}

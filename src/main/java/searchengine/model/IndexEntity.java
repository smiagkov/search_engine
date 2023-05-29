package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "indexes")
@Data
@RequiredArgsConstructor
public class IndexEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @ManyToOne
    @JoinColumn(name = "page_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_INDEX_PAGE_ID",
                    foreignKeyDefinition = "FOREIGN KEY (`page_id`) REFERENCES `page`(`id`) ON DELETE CASCADE ON UPDATE NO ACTION"))
    private final PageEntity page;
    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false,
            foreignKey = @ForeignKey(name = "FK_INDEX_LEMMA_ID",
                    foreignKeyDefinition = "FOREIGN KEY (`lemma_id`) REFERENCES `lemma`(`id`) ON DELETE CASCADE ON UPDATE NO ACTION"))
    private final LemmaEntity lemma;
    @Column(nullable = false)
    private final float rank;
}

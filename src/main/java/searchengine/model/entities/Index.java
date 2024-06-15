package searchengine.model.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;


@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Table(name = "search_index")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Index {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long Id;

    @ManyToOne
    Page page;

    @ManyToOne
    Lemma lemma;

    @Column(name = "`rank`")
    Integer rank;

}

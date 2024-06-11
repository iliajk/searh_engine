package searchengine.model.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Objects;


@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    WebSite webSite;
    @Column(columnDefinition = "VARCHAR(255)")
    String lemma;

    Integer frequency;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lemma lemma1 = (Lemma) o;
        return webSite.equals(lemma1.webSite) && lemma.equals(lemma1.lemma);
    }

    @Override
    public int hashCode() {
        return Objects.hash(webSite, lemma);
    }
}

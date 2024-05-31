package searchengine.model.entities;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Objects;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
@Table(indexes = {
        @Index(name = "idx_path", columnList = "path")
})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    WebSite webSite;

    String path;

    Integer code;

    @Column(columnDefinition = "MEDIUMTEXT")
    String content;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Page page = (Page) o;
        return path.equals(page.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}

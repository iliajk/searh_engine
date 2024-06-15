package searchengine.model.entities;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;
import searchengine.model.enums.Status;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "site")
public class WebSite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    Status status;
    @UpdateTimestamp
    LocalDateTime statusTime;
    @Column(columnDefinition = "text")
    String last_error;
    @Column(columnDefinition = "VARCHAR(255)")
    String name;
    @Column(columnDefinition = "VARCHAR(255)")
    String url;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebSite webSite = (WebSite) o;
        return name.equals(webSite.name) && url.equals(webSite.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url);
    }
}

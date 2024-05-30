package searchengine.model.entities;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
} )
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    WebSite webSite;

    String path;

    Integer code;

    @Column(columnDefinition = "MEDIUMTEXT")
    String content;
}

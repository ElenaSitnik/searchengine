package searchengine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "page")
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "site_id", referencedColumnName = "id")
    private Site site;

    @Column(name = "path")
    private String path;

    @Column(name = "code")
    private int code;

    @Column(name = "content")
    private String content;

    @OneToMany
    @JoinColumn(name = "page_id")
    private List<Index> indexesList = new ArrayList<>();

}

package org.qubership.colly.db.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "namespaces")
public class Namespace extends PanacheEntityBase {
    @Id
    public String uid;

    public String name;
    @ManyToOne()
    @JoinColumn(referencedColumnName = "name")
    public Cluster cluster;

    @ManyToOne()
    @JoinColumn(referencedColumnName = "id")
    @JsonIgnore
    public Environment environment;

}

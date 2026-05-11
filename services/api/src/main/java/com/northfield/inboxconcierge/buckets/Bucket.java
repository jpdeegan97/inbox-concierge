package com.northfield.inboxconcierge.buckets;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "buckets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Bucket {
    @Id
    private String id;
    private String name;
    private String description;
    private Integer priority;
    private boolean isDefault;
    private boolean createdByUser;
}

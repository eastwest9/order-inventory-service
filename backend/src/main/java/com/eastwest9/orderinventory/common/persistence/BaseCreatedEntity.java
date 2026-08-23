package com.eastwest9.orderinventory.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
public abstract class BaseCreatedEntity {

    @CreationTimestamp(source = SourceType.DB)
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "created_id",
            length = 36,
            updatable = false
    )
    private String createdId;
}

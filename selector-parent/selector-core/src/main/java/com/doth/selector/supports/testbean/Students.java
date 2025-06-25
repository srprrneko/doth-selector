package com.doth.selector.supports.testbean;

import com.doth.selector.anno.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Students {
    private Integer studentId;
    private String name;
    private Integer age;
    private Timestamp createdAt;
}

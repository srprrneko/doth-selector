// src/main/java/com/doth/selector/supports/sqlgenerator/dto/DTOJoinInfo.java
package com.doth.selector.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

/**
 * DTO 对应的一组 JoinDef 预注册信息。
 */
@AllArgsConstructor
@Getter
public class DTOJoinInfo {
    private final List<JoinDef> joinDefs;
}

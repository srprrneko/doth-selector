// src/main/java/com/doth/selector/supports/sql/dto/DTOJoinInfo.java
package com.doth.selector.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * DTO 对应的一组 JoinDefInfo 预注册信息。
 */
@AllArgsConstructor
@Getter
public class DTOJoinInfo {
    private final List<JoinDefInfo> joinDefInfos;
}

package com.doth.selector.supports.testbean;

public class StatDTO {
    private Long total;
    private Integer maxAge;


    public StatDTO() {
    }

    public StatDTO(Long total, Integer maxAge) {
        this.total = total;
        this.maxAge = maxAge;
    }

    /**
     * 获取
     * @return total
     */
    public Long getTotal() {
        return total;
    }

    /**
     * 设置
     * @param total
     */
    public void setTotal(Long total) {
        this.total = total;
    }

    /**
     * 获取
     * @return maxAge
     */
    public Integer getMaxAge() {
        return maxAge;
    }

    /**
     * 设置
     * @param maxAge
     */
    public void setMaxAge(Integer maxAge) {
        this.maxAge = maxAge;
    }

    public String toString() {
        return "StatDTO{total = " + total + ", maxAge = " + maxAge + "}";
    }
}
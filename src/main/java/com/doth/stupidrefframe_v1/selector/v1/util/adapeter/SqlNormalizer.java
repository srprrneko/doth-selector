package com.doth.stupidrefframe_v1.selector.v1.util.adapeter;

/**
 * @project: test02
 * @package: com.doth.stupidrefframe_v1.selector.v1.supports.adapeter
 * @author: doth
 * @creTime: 2025-04-02  01:32
 * @desc: 负责
 * @v: 1.0
 */
public class SqlNormalizer {


    /*
        移动适配器曾到util层
        问题: 现在的 adapter 是属于适配器层
            -> 协调sql执行前, 规范校验
            -> 实体封装成sql之间

            也就是说, 怎么看这个适配器曾都属于工具层, 与多方组合, 辅助多方

        问题1: 现在的 builder 层, 属于{协调层{辅助层}}, 这样感觉有点乱
            -> builder 是执行器的特性, 用于做条件构建
            -> 门面中用到, 执行器中用到, 协调器中也有, 是项目的核心
            -> 但是它主要是服务于builder执行器, 属于builder执行的, 因builder执行而生, 所以可以考虑将这个玩意移动到builder执行器中, 这样是否妥当?
            -> 可是看向executor层, 包含两个包, 分别是基础班单表查询(或dto), 和多表联查版, 都用到了builder, 我总不能把builder拆分开来吧,
            -> 也就是说, 执行器与builder之间是组合关系, 被两种执行器作为参数, 做具体的build逻辑处理
            -> 可能考虑在执行器中新建一个包叫做supports, 然后将builder放在里面, 这样他们的关系就是平级的了

        问题2: 现在的util层和协调器层中的sql层职责模糊, 尤其是sqlbuilder,
            -> sqlbuilder负责的是sql生成的辅助, 生成where子句; 生成基础sql

            -> 他们之间应该是聚合关系, 依赖有些强

            -> util层的功能主要是sql生成的辅助, 但是像是EntityAdapter 这样的适配层多个地方都用得到, 所以util层结构保留
            -> 想这些sql 生成相关的可以考虑放在协调器里, 因为只有这个地方会用到,
            -> 但好像不是, 像命名转换这种发多方都使用的应该保留在原始的util层
            -> 只有别名生成, sql生成这部分完全服务于sql生成的门面类, 他们之间的关系最好应该是聚合关系, 依赖关系强

            -> 但是这个抽象的条件解析是属于哪一方? 它是完完全全服务于sqlbuilder的辅助层, 这样一来可以发现sqlbuilder的职责有些混乱, 后续可能发生改变?
            -> 似乎不用发生改变, 这个类确确实实也是干着辅助生成sql语句的工作, 但是这或许引起了某种歧义? executor层中也有生成sql 的工作
                -> selectGenerateFacade 也有生成sql 的工作, sqlbuilder也有生成sql 的工作, 先把executor层的sql生成工作移到selectGenerateFacade, 然后sqlbuilder辅助它
                -> 这样的话, 双方的职责就清晰多了, selectGenerateFacade 负责多方协调整合工作, 用于最终sql 的生成, builder和它是最好应该是聚合关系

        问题3: 新结构下的 coordinator/supports/sqlGenerator, 结构该如何安排
            ->　首先是多个生成sql的工具, 和门面类selectGenerateFacade 是组合关系, sqlbuilder是强依赖关系, conditionResolverUtil是服务于sqlbuilder的
     */
    // ----------------- "*" 号替换白名单 -----------------
    public static String replaceWildcard(String sql, String whiteList) {
        return sql.replaceAll("(?i)select\\s+\\*", "select " + whiteList);
    }


}

package com.doth.selector.anno.processor.core.dtogenerate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.doth.selector.anno.processor.core.dtogenerate.JNNameResolver.getOrD4JNLevelAttrName;

class ChainProcessContext {

        /**
         * 查询列列表, 包含内容例: t0.name, t1.name..
         */
        List<JoinInfo> joinInfos = new ArrayList<>();

        /**
         * 用于装载join子句的信息最终逐一封装成 JoinInfo 并填充至JoinChainResult最后返回
         */
        List<String> selectColList = new ArrayList<>();


        /**
         * 前缀到别名的键值对
         */
        Map<String, String> prefix2Alias = new HashMap<>();

        /**
         * 处理链
         */
        boolean chain = false;

        /**
         * 当前处理的属性名, 对应 J/N level 的 attrName
         */
        String currentAttrName;

        /**
         * 层级深度
         */
        /*private*/ int level = 1;

        /**
         * 为 joinLevel 开启一条新的 join 链
         *
         * @param info 参数信息
         */
        void startNewChain(ParamInfo info) {
            // 当前层级延续, 并清空上一层级
            this.chain = true;
            this.prefix2Alias.clear();

            String alias = allocateAlias(info.prefix);
            this.currentAttrName = getOrD4JNLevelAttrName(info.jl);
            this.joinInfos.add(new JoinInfo(currentAttrName, alias));
            this.selectColList.add(alias + "." + info.originName);
        }

        /**
         * 为 Next 延续已有 join 链
         *
         * @param info 参数信息
         */
        void extendCurrentChain(ParamInfo info) {
            String alias = allocateAlias(info.prefix);

            // 将当前层级 attrName 延续上一个层级
            this.currentAttrName = currentAttrName + "." + getOrD4JNLevelAttrName(info.nx); // 例: department.company
            this.joinInfos.add(new JoinInfo(currentAttrName, alias));
            this.selectColList.add(alias + "." + info.originName);
        }

        /**
         * 分配 tN 别名
         *
         * @param prefix 前缀, 对应 info.prefix
         * @return 前缀对应的 tN 别名
         */
        /*private*/ String allocateAlias(String prefix) { // protect
            // 准备 tN 前缀
            String alias = "t" + level++;
            prefix2Alias.put(prefix, alias);
            return alias;
        }

    }

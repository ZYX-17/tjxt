package com.tianji.aigc.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVO {

    private String associationalWord = """
        你是一个创意联想助手。根据用户输入的关键词，生成3个相关的延伸问题。
        
        【规则】
        1. 每个问题必须包含用户输入的关键词
        2. 每个问题不超过20字
        3. 问题之间用"|"分隔
        4. 只输出问题内容，不要任何解释
        
        【示例】
        输入：Java
        输出：Java适合初学者吗？|Java开发需要什么工具？|Java和Python哪个更好学？
        
        【用户输入】
        $input
        """;

    private String helpedWrite = """
        根据用户输入的主题，生成一篇结构完整的短文，要求有开头、主体、结尾，语言专业易懂。
        
        用户输入：
        $input
        """;

    private String continuedWrite = """
        你是一个写作辅助助手。请根据用户提供的已有文本，自然地延续写作。
        
        【要求】
        1. 保持与原文一致的写作风格和语气
        2. 延续原文的逻辑思路，不跳跃
        3. 续写内容长度与原文相当
        4. 只输出续写内容，不要重复原文
        
        【用户已有文本】
        $input
        """;

    private String polish = """
        你是一个文字润色专家。请对用户提供的文本进行润色优化。
        
        【优化方向】
        1. 调整句式结构，使表达更流畅
        2. 替换不够精准的词汇
        3. 统一行文风格，消除口语化或不规范表达
        4. 保持原意不变，不增删核心信息
        
        【用户原文】
        $input
        
        【润色后】
        """;

    private String streamline = """
        你是一个信息提炼专家。请将用户提供的长文本压缩为简洁版本。
        
        【要求】
        1. 保留核心观点和关键信息
        2. 删除冗余表达和重复内容
        3. 压缩后字数不超过原文的50%
        4. 用精炼的语言重新组织，而非简单删减
        
        【用户原文】
        $input
        
        【精简后】
        """;
}
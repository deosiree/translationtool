from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity
import unittest

# 加载BGE Large模型
model = SentenceTransformer('BAAI/bge-large-en')


def calculate_similarity(query, candidates, top=3):
    '''
    计算查询词条和候选词条的相似度
    query: 查询词条
    candidates: 候选词条列表
    top: 前top个结果
    return: 前top个结果的列表
    '''
    # 对查询词条和候选词条进行编码
    query_embedding = model.encode(query)
    candidate_embeddings = model.encode(candidates)

    # 计算余弦相似度
    similarities = cosine_similarity([query_embedding], candidate_embeddings)[0]

    # 输出每个候选词条的相似度
    for i, candidate in enumerate(candidates):
        print(f"查询词条: {query}")
        print(f"候选词条: {candidate}")
        print(f"相似度: {similarities[i]}")
        print("-" * 50)

    # 创建一个包含候选句子和相似度的元组列表
    similarity_pairs = list(zip(candidates, similarities))

    # 按照相似度从高到低排序
    similarity_pairs.sort(key=lambda x: x[1], reverse=True)

    # 取前top个结果
    top_pairs = similarity_pairs[:top]

    # 提取前top个句子
    top_sentences = [pair[0] for pair in top_pairs]

    return top_sentences


class TestSimilarity(unittest.TestCase):

    def test_calculate_similarity_nom(self):
        # 正常情况测试
        query = "这是一个查询词条"
        candidates = ["这是一个候选词条 1", "这是一个候选词条 2", "这是一个完全不同的词条"]
        result = calculate_similarity(query, candidates)
        self.assertIsNotNone(result)  # 单元测试的断言，若是None则测试失败，抛出AssertionError异常

    def test_calculate_similarity_boundary(self):
        # 边界情况测试
        query = ""
        candidates = [""]
        result = calculate_similarity(query, candidates)
        self.assertIsNotNone(result)

    def test_calculate_similarity_error(self):
        # 异常情况测试
        query = "这是一个查询词条"
        candidates = None
        with self.assertRaises(TypeError):
            calculate_similarity(query, candidates)

    def test_calculate_similarity_zero_top(self):
        # 测试 top 为 0
        query = "这是一个查询词条"
        candidates = ["这是一个候选词条 1", "这是一个候选词条 2", "这是一个候选词条 3"]
        top = 0

        result = calculate_similarity(query, candidates, top)

        assert len(result) == 0

    def test_calculate_similarity_less_candidates_than_top(self):
        # 测试候选词条数量小于 top
        query = "这是一个查询词条"
        candidates = ["这是一个候选词条 1"]
        top = 2

        result = calculate_similarity(query, candidates, top)

        assert len(result) == 1

    def test_calculate_similarity_nom_en(self):
        # 英文情况测试
        query = "This is a query"
        candidates =["This is candidate entry 1","This is candidate entry 2","This is candidate entry 3"]
        result = calculate_similarity(query, candidates)
        self.assertIsNotNone(result)  # 单元测试的断言，若是None则测试失败，抛出AssertionError异常

    def test_calculate_similarity_nom_fr(self):
        # 法文情况测试
        query = "Ceci est une enquête"
        candidates =["Ceci est une entrée candidate 1", "Ceci est une entrée candidate 2", "Ceci est une entrée candidate 3"]
        result = calculate_similarity(query, candidates)
        self.assertIsNotNone(result)  # 单元测试的断言，若是None则测试失败，抛出AssertionError异常


if __name__ == '__main__':
    unittest.main()

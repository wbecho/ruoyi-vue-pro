package cn.iocoder.yudao.module.system.service.dept;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.post.PostPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.post.PostSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.PostMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

@Import(PostServiceImpl.class)
public class PostServiceImplTest extends BaseDbUnitTest {

    @Resource
    private PostServiceImpl postService;

    @Resource
    private PostMapper postMapper;

    @Test
    public void testCreatePost_success() {
        // 准备参数
        PostSaveReqVO reqVO = randomPojo(PostSaveReqVO.class,
                o -> o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()))
                .setId(null);

        // 调用
        Long postId = postService.createPost(reqVO);
        // 断言
        assertNotNull(postId);
        PostDO post = postMapper.selectById(postId);
        assertPojoEquals(reqVO, post, "id");
    }

    @Test
    public void testCreatePost_nameDuplicate() {
        // mock 数据
        PostDO dbPost = randomPostDO();
        postMapper.insert(dbPost);
        // 准备参数
        PostSaveReqVO reqVO = randomPojo(PostSaveReqVO.class,
                o -> o.setName(dbPost.getName()));

        // 调用，断言异常
        assertServiceException(() -> postService.createPost(reqVO), POST_NAME_DUPLICATE);
    }

    @Test
    public void testCreatePost_codeDuplicate() {
        // mock 数据
        PostDO dbPost = randomPostDO();
        postMapper.insert(dbPost);
        // 准备参数
        PostSaveReqVO reqVO = randomPojo(PostSaveReqVO.class,
                o -> o.setCode(dbPost.getCode()));

        // 调用，断言异常
        assertServiceException(() -> postService.createPost(reqVO), POST_CODE_DUPLICATE);
    }

    @Test
    public void testUpdatePost_success() {
        // mock 数据
        PostDO dbPost = randomPostDO();
        postMapper.insert(dbPost);
        // 准备参数
        PostSaveReqVO reqVO = randomPojo(PostSaveReqVO.class, o -> {
            o.setId(dbPost.getId());
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus());
        });

        // 调用
        postService.updatePost(reqVO);
        // 断言
        PostDO post = postMapper.selectById(reqVO.getId());
        assertPojoEquals(reqVO, post);
    }

    @Test
    public void testUpdatePost_notFound() {
        // 准备参数
        PostSaveReqVO reqVO = randomPojo(PostSaveReqVO.class,
                o -> o.setId(randomLongId()));

        // 调用，断言异常
        assertServiceException(() -> postService.updatePost(reqVO), POST_NOT_FOUND);
    }

    @Test
    public void testUpdatePost_nameDuplicate() {
        // mock 数据
        PostDO dbPost1 = randomPostDO();
        postMapper.insert(dbPost1);
        PostDO dbPost2 = randomPostDO();
        postMapper.insert(dbPost2);
        // 准备参数 - 更新 post2 的名字为 post1 的名字
        PostSaveReqVO reqVO = randomPojo(PostSaveReqVO.class, o -> {
            o.setId(dbPost2.getId());
            o.setName(dbPost1.getName());
        });

        // 调用，断言异常
        assertServiceException(() -> postService.updatePost(reqVO), POST_NAME_DUPLICATE);
    }

    @Test
    public void testUpdatePost_codeDuplicate() {
        // mock 数据
        PostDO dbPost1 = randomPostDO();
        postMapper.insert(dbPost1);
        PostDO dbPost2 = randomPostDO();
        postMapper.insert(dbPost2);
        // 准备参数 - 更新 post2 的编码为 post1 的编码
        PostSaveReqVO reqVO = randomPojo(PostSaveReqVO.class, o -> {
            o.setId(dbPost2.getId());
            o.setCode(dbPost1.getCode());
        });

        // 调用，断言异常
        assertServiceException(() -> postService.updatePost(reqVO), POST_CODE_DUPLICATE);
    }

    @Test
    public void testDeletePost_success() {
        // mock 数据
        PostDO dbPost = randomPostDO();
        postMapper.insert(dbPost);
        // 准备参数
        Long id = dbPost.getId();

        // 调用
        postService.deletePost(id);
        // 断言
        assertNull(postMapper.selectById(id));
    }

    @Test
    public void testDeletePost_notFound() {
        // 准备参数
        Long id = randomLongId();

        // 调用，断言异常
        assertServiceException(() -> postService.deletePost(id), POST_NOT_FOUND);
    }

    @Test
    public void testGetPost() {
        // mock 数据
        PostDO dbPost = randomPostDO();
        postMapper.insert(dbPost);
        // 准备参数
        Long id = dbPost.getId();

        // 调用
        PostDO post = postService.getPost(id);
        // 断言
        assertPojoEquals(dbPost, post);
    }

    @Test
    public void testGetPostList() {
        // mock 数据
        PostDO post1 = randomPostDO();
        postMapper.insert(post1);
        PostDO post2 = randomPostDO();
        postMapper.insert(post2);
        // 准备参数
        Collection<Long> ids = List.of(post1.getId(), post2.getId());

        // 调用
        List<PostDO> list = postService.getPostList(ids);
        // 断言
        assertEquals(2, list.size());
    }

    @Test
    public void testGetPostPage() {
        // mock 数据
        PostDO dbPost = randomPojo(PostDO.class, o -> {
            o.setName("芋艿");
            o.setCode("yunai");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        postMapper.insert(dbPost);
        // 测试 name 不匹配
        postMapper.insert(cloneIgnoreId(dbPost, o -> o.setName("艿")));
        // 测试 code 不匹配
        postMapper.insert(cloneIgnoreId(dbPost, o -> o.setCode("nai")));
        // 测试 status 不匹配
        postMapper.insert(cloneIgnoreId(dbPost, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // 准备参数
        PostPageReqVO reqVO = new PostPageReqVO();
        reqVO.setName("芋");
        reqVO.setCode("yu");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        // 调用
        PageResult<PostDO> pageResult = postService.getPostPage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbPost, pageResult.getList().get(0));
    }

    @Test
    public void testValidatePostList_success() {
        // mock 数据
        PostDO dbPost = randomPostDO(o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        postMapper.insert(dbPost);
        // 准备参数
        Collection<Long> ids = List.of(dbPost.getId());

        // 调用，不会抛异常
        postService.validatePostList(ids);
    }

    @Test
    public void testValidatePostList_notFound() {
        // 准备参数
        Collection<Long> ids = List.of(randomLongId());

        // 调用，断言异常
        assertServiceException(() -> postService.validatePostList(ids), POST_NOT_FOUND);
    }

    @Test
    public void testValidatePostList_notEnable() {
        // mock 数据
        PostDO dbPost = randomPostDO(o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        postMapper.insert(dbPost);
        // 准备参数
        Collection<Long> ids = List.of(dbPost.getId());

        // 调用，断言异常
        assertServiceException(() -> postService.validatePostList(ids), POST_NOT_ENABLE, dbPost.getName());
    }

    // ========== 随机对象 ==========

    @SafeVarargs
    private static PostDO randomPostDO(Consumer<PostDO>... consumers) {
        Consumer<PostDO> consumer = (o) -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus());
        };
        return randomPojo(PostDO.class, ArrayUtils.append(consumer, consumers));
    }
}

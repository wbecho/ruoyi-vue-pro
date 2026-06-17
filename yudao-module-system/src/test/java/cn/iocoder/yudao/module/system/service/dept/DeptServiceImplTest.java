package cn.iocoder.yudao.module.system.service.dept;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Import(DeptServiceImpl.class)
public class DeptServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DeptServiceImpl deptService;

    @Resource
    private DeptMapper deptMapper;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @MockBean
    private ValueOperations<String, Object> valueOperations;

    @Test
    public void testCreateDept_success() {
        // 准备参数
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class,
                o -> o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()))
                .setId(null);
        // 如果设置了父部门，需要 mock 父部门存在
        if (reqVO.getParentId() != null && !DeptDO.PARENT_ID_ROOT.equals(reqVO.getParentId())) {
            DeptDO parentDept = randomPojo(DeptDO.class, o -> {
                o.setId(reqVO.getParentId());
                o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            });
            deptMapper.insert(parentDept);
        }

        // 调用
        Long deptId = deptService.createDept(reqVO);
        // 断言
        assertNotNull(deptId);
        DeptDO dept = deptMapper.selectById(deptId);
        assertPojoEquals(reqVO, dept, "id");
    }

    @Test
    public void testCreateDept_parentNotExists() {
        // 准备参数
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class,
                o -> o.setParentId(randomLongId())); // 设置一个不存在的父部门

        // 调用，断言异常
        assertServiceException(() -> deptService.createDept(reqVO), DEPT_PARENT_NOT_EXITS);
    }

    @Test
    public void testCreateDept_parentIsSelf() {
        // 准备参数
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class,
                o -> o.setId(1L).setParentId(1L)); // 设置自己为父部门

        // 调用，断言异常
        assertServiceException(() -> deptService.createDept(reqVO), DEPT_PARENT_ERROR);
    }

    @Test
    public void testCreateDept_nameDuplicate() {
        // mock 数据
        DeptDO dbDept = randomDeptDO();
        deptMapper.insert(dbDept);
        // 准备参数
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class,
                o -> o.setParentId(dbDept.getParentId()).setName(dbDept.getName()));

        // 调用，断言异常
        assertServiceException(() -> deptService.createDept(reqVO), DEPT_NAME_DUPLICATE);
    }

    @Test
    public void testUpdateDept_success() {
        // mock 数据
        DeptDO dbDept = randomDeptDO();
        deptMapper.insert(dbDept);
        // 准备参数
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class, o -> {
            o.setId(dbDept.getId());
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus());
        });

        // 调用
        deptService.updateDept(reqVO);
        // 断言
        DeptDO dept = deptMapper.selectById(reqVO.getId());
        assertPojoEquals(reqVO, dept);
    }

    @Test
    public void testUpdateDept_notFound() {
        // 准备参数
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class,
                o -> o.setId(randomLongId()));

        // 调用，断言异常
        assertServiceException(() -> deptService.updateDept(reqVO), DEPT_NOT_FOUND);
    }

    @Test
    public void testUpdateDept_parentIsChild() {
        // mock 数据 - 创建父部门
        DeptDO parentDept = randomDeptDO();
        deptMapper.insert(parentDept);
        // mock 数据 - 创建子部门
        DeptDO childDept = randomDeptDO(o -> o.setParentId(parentDept.getId()));
        deptMapper.insert(childDept);

        // 准备参数 - 设置子部门的父部门为自己，形成循环
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class, o -> {
            o.setId(parentDept.getId());
            o.setParentId(childDept.getId()); // 设置子部门为父部门，会形成循环
        });

        // 调用，断言异常
        assertServiceException(() -> deptService.updateDept(reqVO), DEPT_PARENT_IS_CHILD);
    }

    @Test
    public void testDeleteDept_success() {
        // mock 数据
        DeptDO dbDept = randomDeptDO();
        deptMapper.insert(dbDept);
        // 准备参数
        Long id = dbDept.getId();

        // 调用
        deptService.deleteDept(id);
        // 断言
        assertNull(deptMapper.selectById(id));
    }

    @Test
    public void testDeleteDept_notFound() {
        // 准备参数
        Long id = randomLongId();

        // 调用，断言异常
        assertServiceException(() -> deptService.deleteDept(id), DEPT_NOT_FOUND);
    }

    @Test
    public void testDeleteDept_hasChildren() {
        // mock 数据 - 父部门
        DeptDO parentDept = randomDeptDO();
        deptMapper.insert(parentDept);
        // mock 数据 - 子部门
        DeptDO childDept = randomDeptDO(o -> o.setParentId(parentDept.getId()));
        deptMapper.insert(childDept);
        // 准备参数
        Long id = parentDept.getId();

        // 调用，断言异常
        assertServiceException(() -> deptService.deleteDept(id), DEPT_EXITS_CHILDREN);
    }

    @Test
    public void testGetDept() {
        // mock 数据
        DeptDO dbDept = randomDeptDO();
        deptMapper.insert(dbDept);
        // 准备参数
        Long id = dbDept.getId();

        // 调用
        DeptDO dept = deptService.getDept(id);
        // 断言
        assertPojoEquals(dbDept, dept);
    }

    @Test
    public void testGetDeptList() {
        // mock 数据
        DeptDO dept1 = randomDeptDO();
        deptMapper.insert(dept1);
        DeptDO dept2 = randomDeptDO();
        deptMapper.insert(dept2);
        // 准备参数
        Collection<Long> ids = List.of(dept1.getId(), dept2.getId());

        // 调用
        List<DeptDO> list = deptService.getDeptList(ids);
        // 断言
        assertEquals(2, list.size());
    }

    @Test
    public void testGetChildDeptList() {
        // mock 数据 - 父部门
        DeptDO parentDept = randomDeptDO();
        deptMapper.insert(parentDept);
        // mock 数据 - 子部门
        DeptDO childDept = randomDeptDO(o -> o.setParentId(parentDept.getId()));
        deptMapper.insert(childDept);
        // mock 数据 - 孙子部门
        DeptDO grandchildDept = randomDeptDO(o -> o.setParentId(childDept.getId()));
        deptMapper.insert(grandchildDept);
        // 准备参数
        Collection<Long> ids = List.of(parentDept.getId());

        // 调用
        List<DeptDO> children = deptService.getChildDeptList(ids);
        // 断言
        assertEquals(2, children.size());
    }

    @Test
    public void testGetChildDeptIdListFromCache() {
        // mock 数据 - 父部门
        DeptDO parentDept = randomDeptDO();
        deptMapper.insert(parentDept);
        // mock 数据 - 子部门
        DeptDO childDept = randomDeptDO(o -> o.setParentId(parentDept.getId()));
        deptMapper.insert(childDept);
        // 准备参数
        Long id = parentDept.getId();
        // mock redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        // 调用
        Set<Long> childIds = deptService.getChildDeptIdListFromCache(id);
        // 断言
        assertTrue(childIds.contains(childDept.getId()));
    }

    @Test
    public void testValidateDeptList_success() {
        // mock 数据
        DeptDO dbDept = randomDeptDO(o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        deptMapper.insert(dbDept);
        // 准备参数
        Collection<Long> ids = List.of(dbDept.getId());

        // 调用，不会抛异常
        deptService.validateDeptList(ids);
    }

    @Test
    public void testValidateDeptList_notFound() {
        // 准备参数
        Collection<Long> ids = List.of(randomLongId());

        // 调用，断言异常
        assertServiceException(() -> deptService.validateDeptList(ids), DEPT_NOT_FOUND);
    }

    @Test
    public void testValidateDeptList_notEnable() {
        // mock 数据
        DeptDO dbDept = randomDeptDO(o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        deptMapper.insert(dbDept);
        // 准备参数
        Collection<Long> ids = List.of(dbDept.getId());

        // 调用，断言异常
        assertServiceException(() -> deptService.validateDeptList(ids), DEPT_NOT_ENABLE, dbDept.getName());
    }

    // ========== 随机对象 ==========

    @SafeVarargs
    private static DeptDO randomDeptDO(Consumer<DeptDO>... consumers) {
        Consumer<DeptDO> consumer = (o) -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT); // 默认父部门为根
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus());
        };
        return randomPojo(DeptDO.class, ArrayUtils.append(consumer, consumers));
    }
}

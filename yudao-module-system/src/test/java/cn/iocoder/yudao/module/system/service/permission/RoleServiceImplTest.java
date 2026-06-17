package cn.iocoder.yudao.module.system.service.permission;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RolePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.role.RoleSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import cn.iocoder.yudao.module.system.enums.permission.RoleTypeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@Import(RoleServiceImpl.class)
public class RoleServiceImplTest extends BaseDbUnitTest {

    @Resource
    private RoleServiceImpl roleService;

    @Resource
    private RoleMapper roleMapper;

    @MockBean
    private PermissionService permissionService;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testCreateRole_success() {
        // 准备参数
        RoleSaveReqVO reqVO = randomPojo(RoleSaveReqVO.class,
                o -> o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()))
                .setId(null);

        // 调用
        Long roleId = roleService.createRole(reqVO, null);
        // 断言
        assertNotNull(roleId);
        RoleDO role = roleMapper.selectById(roleId);
        assertPojoEquals(reqVO, role, "id");
        assertEquals(RoleTypeEnum.CUSTOM.getType(), role.getType());
    }

    @Test
    public void testCreateRole_nameDuplicate() {
        // mock 数据
        RoleDO dbRole = randomRoleDO();
        roleMapper.insert(dbRole);
        // 准备参数
        RoleSaveReqVO reqVO = randomPojo(RoleSaveReqVO.class,
                o -> o.setName(dbRole.getName()));

        // 调用，断言异常
        assertServiceException(() -> roleService.createRole(reqVO, null), ROLE_NAME_DUPLICATE, dbRole.getName());
    }

    @Test
    public void testCreateRole_codeDuplicate() {
        // mock 数据
        RoleDO dbRole = randomRoleDO();
        roleMapper.insert(dbRole);
        // 准备参数
        RoleSaveReqVO reqVO = randomPojo(RoleSaveReqVO.class,
                o -> o.setCode(dbRole.getCode()));

        // 调用，断言异常
        assertServiceException(() -> roleService.createRole(reqVO, null), ROLE_CODE_DUPLICATE, dbRole.getCode());
    }

    @Test
    public void testCreateRole_superAdminCode() {
        // 准备参数 - 使用超级管理员的 code
        RoleSaveReqVO reqVO = randomPojo(RoleSaveReqVO.class,
                o -> o.setCode(RoleCodeEnum.SUPER_ADMIN.getCode()));

        // 调用，断言异常
        assertServiceException(() -> roleService.createRole(reqVO, null), ROLE_ADMIN_CODE_ERROR,
                RoleCodeEnum.SUPER_ADMIN.getCode());
    }

    @Test
    public void testUpdateRole_success() {
        // mock 数据
        RoleDO dbRole = randomRoleDO();
        dbRole.setType(RoleTypeEnum.CUSTOM.getType()); // 确保不是系统内置角色
        roleMapper.insert(dbRole);
        // 准备参数
        RoleSaveReqVO reqVO = randomPojo(RoleSaveReqVO.class, o -> {
            o.setId(dbRole.getId());
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus());
        });

        // 调用
        roleService.updateRole(reqVO);
        // 断言
        RoleDO role = roleMapper.selectById(reqVO.getId());
        assertPojoEquals(reqVO, role);
    }

    @Test
    public void testUpdateRole_notFound() {
        // 准备参数
        RoleSaveReqVO reqVO = randomPojo(RoleSaveReqVO.class,
                o -> o.setId(randomLongId()));

        // 调用，断言异常
        assertServiceException(() -> roleService.updateRole(reqVO), ROLE_NOT_EXISTS);
    }

    @Test
    public void testUpdateRole_systemTypeRole() {
        // mock 数据 - 系统内置角色
        RoleDO dbRole = randomRoleDO();
        dbRole.setType(RoleTypeEnum.SYSTEM.getType());
        roleMapper.insert(dbRole);
        // 准备参数
        RoleSaveReqVO reqVO = randomPojo(RoleSaveReqVO.class,
                o -> o.setId(dbRole.getId()));

        // 调用，断言异常
        assertServiceException(() -> roleService.updateRole(reqVO), ROLE_CAN_NOT_UPDATE_SYSTEM_TYPE_ROLE);
    }

    @Test
    public void testUpdateRole_nameDuplicate() {
        // mock 数据
        RoleDO dbRole1 = randomRoleDO();
        roleMapper.insert(dbRole1);
        RoleDO dbRole2 = randomRoleDO();
        roleMapper.insert(dbRole2);
        // 准备参数 - 更新 role2 的名字为 role1 的名字
        RoleSaveReqVO reqVO = randomPojo(RoleSaveReqVO.class, o -> {
            o.setId(dbRole2.getId());
            o.setName(dbRole1.getName());
        });

        // 调用，断言异常
        assertServiceException(() -> roleService.updateRole(reqVO), ROLE_NAME_DUPLICATE, dbRole1.getName());
    }

    @Test
    public void testUpdateRole_codeDuplicate() {
        // mock 数据
        RoleDO dbRole1 = randomRoleDO();
        roleMapper.insert(dbRole1);
        RoleDO dbRole2 = randomRoleDO();
        roleMapper.insert(dbRole2);
        // 准备参数 - 更新 role2 的编码为 role1 的编码
        RoleSaveReqVO reqVO = randomPojo(RoleSaveReqVO.class, o -> {
            o.setId(dbRole2.getId());
            o.setCode(dbRole1.getCode());
        });

        // 调用，断言异常
        assertServiceException(() -> roleService.updateRole(reqVO), ROLE_CODE_DUPLICATE, dbRole1.getCode());
    }

    @Test
    public void testUpdateRoleDataScope_success() {
        // mock 数据
        RoleDO dbRole = randomRoleDO();
        dbRole.setType(RoleTypeEnum.CUSTOM.getType());
        roleMapper.insert(dbRole);
        // 准备参数
        Long id = dbRole.getId();
        Integer dataScope = 1;
        Set<Long> dataScopeDeptIds = Set.of(1L, 2L);

        // 调用
        roleService.updateRoleDataScope(id, dataScope, dataScopeDeptIds);
        // 断言
        RoleDO role = roleMapper.selectById(id);
        assertEquals(dataScope, role.getDataScope());
        assertEquals(dataScopeDeptIds, role.getDataScopeDeptIds());
    }

    @Test
    public void testDeleteRole_success() {
        // mock 数据
        RoleDO dbRole = randomRoleDO();
        dbRole.setType(RoleTypeEnum.CUSTOM.getType());
        roleMapper.insert(dbRole);
        // 准备参数
        Long id = dbRole.getId();

        // 调用
        roleService.deleteRole(id);
        // 断言
        assertNull(roleMapper.selectById(id));
        verify(permissionService, times(1)).processRoleDeleted(id);
    }

    @Test
    public void testDeleteRole_notFound() {
        // 准备参数
        Long id = randomLongId();

        // 调用，断言异常
        assertServiceException(() -> roleService.deleteRole(id), ROLE_NOT_EXISTS);
    }

    @Test
    public void testDeleteRole_systemTypeRole() {
        // mock 数据 - 系统内置角色
        RoleDO dbRole = randomRoleDO();
        dbRole.setType(RoleTypeEnum.SYSTEM.getType());
        roleMapper.insert(dbRole);
        // 准备参数
        Long id = dbRole.getId();

        // 调用，断言异常
        assertServiceException(() -> roleService.deleteRole(id), ROLE_CAN_NOT_UPDATE_SYSTEM_TYPE_ROLE);
    }

    @Test
    public void testGetRole() {
        // mock 数据
        RoleDO dbRole = randomRoleDO();
        roleMapper.insert(dbRole);
        // 准备参数
        Long id = dbRole.getId();

        // 调用
        RoleDO role = roleService.getRole(id);
        // 断言
        assertPojoEquals(dbRole, role);
    }

    @Test
    public void testGetRoleList() {
        // mock 数据
        RoleDO role1 = randomRoleDO();
        roleMapper.insert(role1);
        RoleDO role2 = randomRoleDO();
        roleMapper.insert(role2);
        // 准备参数
        Collection<Long> ids = List.of(role1.getId(), role2.getId());

        // 调用
        List<RoleDO> list = roleService.getRoleList(ids);
        // 断言
        assertEquals(2, list.size());
    }

    @Test
    public void testGetRolePage() {
        // mock 数据
        RoleDO dbRole = randomPojo(RoleDO.class, o -> {
            o.setName("芋艿");
            o.setCode("yunai");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setCreateTime(buildTime(2021, 1, 15));
        });
        roleMapper.insert(dbRole);
        // 测试 name 不匹配
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setName("艿")));
        // 测试 code 不匹配
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setCode("nai")));
        // 测试 status 不匹配
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // 测试 createTime 不匹配
        roleMapper.insert(cloneIgnoreId(dbRole, o -> o.setCreateTime(buildTime(2021, 1, 1))));
        // 准备参数
        RolePageReqVO reqVO = new RolePageReqVO();
        reqVO.setName("芋");
        reqVO.setCode("yu");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCreateTime(buildBetweenTime(2021, 1, 10, 2021, 1, 20));

        // 调用
        PageResult<RoleDO> pageResult = roleService.getRolePage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbRole, pageResult.getList().get(0));
    }

    @Test
    public void testValidateRoleList_success() {
        // mock 数据
        RoleDO dbRole = randomRoleDO(o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus()));
        roleMapper.insert(dbRole);
        // 准备参数
        Collection<Long> ids = List.of(dbRole.getId());

        // 调用，不会抛异常
        roleService.validateRoleList(ids);
    }

    @Test
    public void testValidateRoleList_notFound() {
        // 准备参数
        Collection<Long> ids = List.of(randomLongId());

        // 调用，断言异常
        assertServiceException(() -> roleService.validateRoleList(ids), ROLE_NOT_EXISTS);
    }

    @Test
    public void testValidateRoleList_notEnable() {
        // mock 数据
        RoleDO dbRole = randomRoleDO(o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        roleMapper.insert(dbRole);
        // 准备参数
        Collection<Long> ids = List.of(dbRole.getId());

        // 调用，断言异常
        assertServiceException(() -> roleService.validateRoleList(ids), ROLE_IS_DISABLE, dbRole.getName());
    }

    // ========== 随机对象 ==========

    @SafeVarargs
    private static RoleDO randomRoleDO(Consumer<RoleDO>... consumers) {
        Consumer<RoleDO> consumer = (o) -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus());
            o.setType(RoleTypeEnum.CUSTOM.getType()); // 默认是自定义角色
        };
        return randomPojo(RoleDO.class, ArrayUtils.append(consumer, consumers));
    }
}

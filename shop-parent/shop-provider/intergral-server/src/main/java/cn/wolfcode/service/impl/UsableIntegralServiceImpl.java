package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.AccountTransaction;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.mapper.AccountTransactionMapper;
import cn.wolfcode.mapper.UsableIntegralMapper;
import cn.wolfcode.service.IUsableIntegralService;
import cn.wolfcode.web.msg.IntergralCodeMsg;
import com.alibaba.fastjson.JSON;
import io.seata.rm.tcc.api.BusinessActionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created by lanxw
 */
@Service
@Slf4j
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Autowired
    private UsableIntegralMapper usableIntegralMapper;
    @Autowired
    private AccountTransactionMapper accountTransactionMapper;

    @Override
    public void decrIntegral(OperateIntergralVo vo) {
        int effectCount = usableIntegralMapper.decrIntergral(vo.getUserId(), vo.getValue());
        if (effectCount == 0) {
            //积分不够抛出异常
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }

    }

    @Override
    public void incrIntegral(OperateIntergralVo vo) {
        usableIntegralMapper.incrIntergral(vo.getUserId(), vo.getValue());
    }

    @Override
    @Transactional
    public void decrIntegralTry(OperateIntergralVo vo, BusinessActionContext context) {

        log.info("执行incrIntegralTry方法");
        //插入事务控制表
        AccountTransaction log = new AccountTransaction();
        log.setTxId(context.getXid());
        log.setActionId(context.getBranchId());
        log.setGmtCreated(new Date());
        log.setGmtModified(new Date());
        log.setUserId(vo.getUserId());
        log.setAmount(vo.getValue());
        accountTransactionMapper.insert(log);
        //积分扣减
        int effectCount = usableIntegralMapper.decrIntergral(vo.getUserId(), vo.getValue());
        if (effectCount == 0) {
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }

    }

    @Override
    public void decrIntegralCommit(BusinessActionContext context) {
        log.info("执行incrIntegralCommit方法");
        //查询事务记录
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if (accountTransaction != null) {
            //如果不为空
            int effectCount = accountTransactionMapper.updateAccountTransactionState(context.getXid(), context.getBranchId(), AccountTransaction.STATE_COMMIT, AccountTransaction.STATE_TRY);

            if (AccountTransaction.STATE_TRY == accountTransaction.getState()) {
                //如果为try，执行commit
            }else if (AccountTransaction.STATE_COMMIT == accountTransaction.getState()) {
                //如果为commit，不做任何事情
            }else {
                //写MQ通知管理员
            }
        }else {
            //写MQ通知管理员
        }
    }

    @Override
    @Transactional
    public void decrIntegralRollback(BusinessActionContext context) {

        log.info("执行incrIntegralRollback方法");
        //查询事务记录
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if (accountTransaction != null) {
            //存在日志记录
            if (AccountTransaction.STATE_TRY == accountTransaction.getState()){
                //处于Try状态
                //将状态修改成Cancel状态
                int effectCount = accountTransactionMapper.updateAccountTransactionState(context.getXid(), context.getBranchId(), AccountTransaction.STATE_CANCEL, AccountTransaction.STATE_TRY);
                //执行cancel业务逻辑，添加积分
                usableIntegralMapper.incrIntergral(accountTransaction.getUserId(), accountTransaction.getAmount());
            }else if (AccountTransaction.STATE_CANCEL == accountTransaction.getState()){
                //之前已经执行过Cancel，幂等处理


            }else {
                //其他情况通知管理员

            }
        }else {
            OperateIntergralVo vo = JSON.parseObject((String) context.getActionContext("vo"), OperateIntergralVo.class);
            //插入事务控制表
            AccountTransaction log = new AccountTransaction();
            log.setTxId(context.getXid());
            log.setActionId(context.getBranchId());
            log.setGmtCreated(new Date());
            log.setGmtModified(new Date());
            log.setUserId(vo.getUserId());
            log.setAmount(vo.getValue());
            log.setState(AccountTransaction.STATE_CANCEL);
            accountTransactionMapper.insert(log);
        }


    }
}

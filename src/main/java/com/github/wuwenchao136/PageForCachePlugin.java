package com.github.wuwenchao136;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.PropertyException;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.apache.log4j.Logger;


/**
 * 
 * @Description: 分页信息查询插件
 * @author     : wuwenchao
 * @date       : 2016年6月22日 上午11:03:18
 *
 */
@Intercepts({
    @Signature(type = Executor.class, method = "query", 
    		args = { MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}) })
public class PageForCachePlugin implements Interceptor {

    private Logger logger = Logger.getLogger(PagePlugin.class);
    private static String dialect = "";
    private static String pageSqlId = "";
    
	public Object intercept(Invocation invocation) throws Throwable {
		 MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
		 if(mappedStatement.getId().matches(pageSqlId)){//根据接口名判断是否为分页查询语句
			 Executor baseExecutor = (Executor) invocation.getTarget();
			 BoundSql boundSql = mappedStatement.getBoundSql(invocation.getArgs()[1]);
			 Object parameterObject = boundSql.getParameterObject();
             if (parameterObject == null) {
                 throw new NullPointerException("parameterObject error");
             } else {
                 Connection connection = baseExecutor.getTransaction().getConnection();
                 String sql = boundSql.getSql();
                 String countSql = "select count(0) from (" + sql + ") myCount";//信息总数查询语句
                 PreparedStatement countStmt = connection
                         .prepareStatement(countSql);
                 BoundSql countBS = new BoundSql(
                         mappedStatement.getConfiguration(), countSql,
                         boundSql.getParameterMappings(), parameterObject);
                 Field metaParamsField = ReflectHelper.getFieldByFieldName(boundSql, "metaParameters");
                 if (metaParamsField != null) {
                     MetaObject mo = (MetaObject) ReflectHelper.getValueByFieldName(boundSql, "metaParameters");
                     ReflectHelper.setValueByFieldName(countBS, "metaParameters", mo);
                 }
                 setParameters(countStmt, mappedStatement, countBS,
                         parameterObject);
                 ResultSet rs = countStmt.executeQuery();//执行查询
                 int count = 0;
                 if (rs.next()) {
                     count = rs.getInt(1);
                 }
                 rs.close();
                 countStmt.close();

                 PageInfo page = null;
                 if (parameterObject instanceof PageInfo) {
                     page = (PageInfo) parameterObject;
                     page.setTotalResult(count);
                 } else if(parameterObject instanceof Map){
                	 @SuppressWarnings("unchecked")
                	 Map<String, Object> map = (Map<String, Object>)parameterObject;
                     page = (PageInfo)map.get("page");
                     if(page == null)
                         page = new PageInfo();
                     page.setTotalResult(count);
                 }else {
                     Field pageField = ReflectHelper.getFieldByFieldName(
                             parameterObject, "page");
                     if (pageField != null) {
                         page = (PageInfo) ReflectHelper.getValueByFieldName(
                                 parameterObject, "page");
                         if (page == null)
                             page = new PageInfo();
                         page.setTotalResult(count);
                         ReflectHelper.setValueByFieldName(parameterObject,
                                 "page", page);
                     } else {
                         throw new NoSuchFieldException(parameterObject
                                 .getClass().getName());
                     }
                 }
                 /*	2015-05-20修改  添加总页数计算	开始   			 */
                 if(page.getTotalResult() > 0) {
         			if(page.getTotalResult() % page.getShowCount() == 0) {
         				page.setTotalPage(page.getTotalResult() / page.getShowCount());
         			} else {
         				page.setTotalPage(page.getTotalResult() / page.getShowCount() + 1);
         			}
                 }
                 /*	2015-05-20修改 	结束			 */

             }
		 }
		 return invocation.proceed();
	}

	public Object plugin(Object target) {
        return Plugin.wrap(target, this);
	}

	public void setProperties(Properties properties) {
		dialect = properties.getProperty("dialect");
        if (dialect ==null || dialect.equals("")) {
            try {
                throw new PropertyException("dialect property is not found!");
            } catch (PropertyException e) {
//                LoggerUtil.error(logger, e, "设置属性异常");
            }
        }
        pageSqlId = properties.getProperty("pageSqlId");
        if (dialect ==null || dialect.equals("")) {
            try {
                throw new PropertyException("pageSqlId property is not found!");
            } catch (PropertyException e) {
//                LoggerUtil.error(logger, e, "设置属性异常");
            }
        }
	}
	
	/**
	 * 设置分页查询相关的查询参数
	 * @param ps
	 * @param mappedStatement
	 * @param boundSql
	 * @param parameterObject
	 * @throws SQLException
	 */
	@SuppressWarnings("unchecked")
	private void setParameters(PreparedStatement ps,
            MappedStatement mappedStatement, BoundSql boundSql,
            Object parameterObject) throws SQLException {
        ErrorContext.instance().activity("setting parameters")
                .object(mappedStatement.getParameterMap().getId());
        List<ParameterMapping> parameterMappings = boundSql
                .getParameterMappings();
        if (parameterMappings != null) {
            Configuration configuration = mappedStatement.getConfiguration();
            TypeHandlerRegistry typeHandlerRegistry = configuration
                    .getTypeHandlerRegistry();
            MetaObject metaObject = parameterObject == null ? null
                    : configuration.newMetaObject(parameterObject);
            for (int i = 0; i < parameterMappings.size(); i++) {
                ParameterMapping parameterMapping = parameterMappings.get(i);
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    PropertyTokenizer prop = new PropertyTokenizer(propertyName);
                    if (parameterObject == null) {
                        value = null;
                    } else if (typeHandlerRegistry
                            .hasTypeHandler(parameterObject.getClass())) {
                        value = parameterObject;
                    } else if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (propertyName.startsWith(ForEachSqlNode.ITEM_PREFIX)
                            && boundSql.hasAdditionalParameter(prop.getName())) {
                        value = boundSql.getAdditionalParameter(prop.getName());
                        if (value != null) {
                            value = configuration.newMetaObject(value)
                                    .getValue(
                                            propertyName.substring(prop
                                                    .getName().length()));
                        }
                    } else {
                        value = metaObject == null ? null : metaObject
                                .getValue(propertyName);
                    }
                    @SuppressWarnings("rawtypes")
					TypeHandler typeHandler = parameterMapping.getTypeHandler();
                    if (typeHandler == null) {
                        throw new ExecutorException(
                                "There was no TypeHandler found for parameter "
                                        + propertyName + " of statement "
                                        + mappedStatement.getId());
                    }
                    typeHandler.setParameter(ps, i + 1, value, parameterMapping.getJdbcType());
                }
            }
        }
    }

}

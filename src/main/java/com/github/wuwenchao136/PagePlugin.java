package com.github.wuwenchao136;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.PropertyException;

import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.log4j.Logger;


/**
 * 
 * @Description: 分页语句拼装插件
 * @author     : wuwenchao
 * @date       : 2016年6月22日 上午11:02:32
 *
 */
@Intercepts({ @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class }) })
public class PagePlugin implements Interceptor {
	private Logger logger = Logger.getLogger(PagePlugin.class);

	private static String dialect = "";
	private static String pageSqlId = "";

	@SuppressWarnings("unchecked")
	public Object intercept(Invocation ivk) throws Throwable {

		if (ivk.getTarget() instanceof RoutingStatementHandler) {
			RoutingStatementHandler statementHandler = (RoutingStatementHandler) ivk
					.getTarget();
			BaseStatementHandler delegate = (BaseStatementHandler) ReflectHelper
					.getValueByFieldName(statementHandler, "delegate");
			MappedStatement mappedStatement = (MappedStatement) ReflectHelper
					.getValueByFieldName(delegate, "mappedStatement");

			if (mappedStatement.getId().matches(pageSqlId)) {//根据接口名判断是否未分页查询语句
				BoundSql boundSql = delegate.getBoundSql();
				Object parameterObject = boundSql.getParameterObject();
				if (parameterObject == null) {
					throw new NullPointerException("parameterObject error");
				} else {
					String sql = boundSql.getSql();
					PageInfo page = null;
					if (parameterObject instanceof PageInfo) {
						page = (PageInfo) parameterObject;
					} else if (parameterObject instanceof Map) {
						Map<String, Object> map = (Map<String, Object>) parameterObject;
						page = (PageInfo) map.get("page");
						if (page == null)
							page = new PageInfo();
					} else {
						Field pageField = ReflectHelper.getFieldByFieldName(
								parameterObject, "page");
						if (pageField != null) {
							page = (PageInfo) ReflectHelper
									.getValueByFieldName(parameterObject,
											"page");
							if (page == null)
								page = new PageInfo();
						} else {
							throw new NoSuchFieldException(parameterObject
									.getClass().getName());
						}
					}
					String pageSql = generatePageSql(sql, page);//拼装分页查询语句
					ReflectHelper.setValueByFieldName(boundSql, "sql", pageSql);
				}
			}
		}
		return ivk.proceed();
	}

	/**
	 * 生成分页sql，支持MySQL及oracle
	 * @param sql	原始sql语句
	 * @param page	分页对象
	 * @return	分页查询sql
	 */
	private String generatePageSql(String sql, PageInfo page) {
		if (page != null && (dialect != null || !dialect.equals(""))) {
			StringBuffer pageSql = new StringBuffer();
			if ("mysql".equals(dialect)) {
				pageSql.append(sql);
				pageSql.append(" limit " + page.getCurrentResult() + ","
						+ page.getShowCount());
			} else if ("oracle".equals(dialect)) {
				pageSql.append("select * from (select tmp_tb.*,ROWNUM row_id from (");
				pageSql.append(sql);
				pageSql.append(")  tmp_tb where ROWNUM<=");
				pageSql.append(page.getCurrentResult() + page.getShowCount());
				pageSql.append(") where row_id>");
				pageSql.append(page.getCurrentResult());
			}
			return pageSql.toString();
		} else {
			return sql;
		}
	}

	public Object plugin(Object arg0) {
		return Plugin.wrap(arg0, this);
	}

	/**
	 * 读取配置文件
	 */
	public void setProperties(Properties p) {
		dialect = p.getProperty("dialect");
		if (dialect == null || dialect.equals("")) {
			try {
				throw new PropertyException("dialect property is not found!");
			} catch (PropertyException e) {
//				LoggerUtil.error(logger, e, "设置属性异常");
			}
		}
		pageSqlId = p.getProperty("pageSqlId");
		if (dialect == null || dialect.equals("")) {
			try {
				throw new PropertyException("pageSqlId property is not found!");
			} catch (PropertyException e) {
//				LoggerUtil.error(logger, e, "设置属性异常");
			}
		}
	}

}

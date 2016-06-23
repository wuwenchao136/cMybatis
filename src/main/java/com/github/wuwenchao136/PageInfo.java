package com.github.wuwenchao136;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 页码信息
 * 
 * @author zhanghaitao
 *
 */
public class PageInfo implements Serializable {

	private static final long serialVersionUID = 587754556498974978L;

	/**
	 * 每一页显示多少,pagesize
	 */
	private int showCount = 10;

	/**
	 * 总页数
	 */
	private int totalPage;

	/**
	 * 总记录数
	 */
	private int totalResult;

	/**
	 * 当前页数 pageNo
	 */
	private int currentPage;

	/**
	 * 当前显示到的ID, 在mysql limit 中就是第一个参数.
	 */
	private int currentResult;

	/**
	 * 
	 * 当前页面相邻的共10个页码
	 * 
	 */
	private List<Integer> adjacent10;

	private String sortField;
	private String order;
	private String scriptPageStr;
	public PageInfo() {
		super();
	}

	public PageInfo(int currentPage) {
		this(10, currentPage);
	}

	public PageInfo(int showCount, int currentPage) {
		super();
		if (showCount <= 0) {
			showCount = 10;
		}
		if (currentPage <= 0) {
			currentPage = 1;
		}
		this.showCount = showCount;
		this.currentPage = currentPage;
		this.currentResult = (this.currentPage - 1) * this.showCount;
		scriptPageStr = getScriptPage();
	}

	public int getShowCount() {
		return showCount;
	}

	public void setShowCount(int showCount) {
		this.showCount = showCount;
		scriptPageStr = getScriptPage();
	}

	public int getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(int totalPage) {
		this.totalPage = totalPage;
		scriptPageStr = getScriptPage();
	}

	public int getTotalResult() {
		return totalResult;
	}

	public void setTotalResult(int totalResult) {
		this.totalResult = totalResult;
		scriptPageStr = getScriptPage();
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
		scriptPageStr = getScriptPage();
	}

	public int getCurrentResult() {
		return currentResult;
	}

	public void setCurrentResult(int currentResult) {
		this.currentResult = currentResult;
		scriptPageStr = getScriptPage();
	}

	public String getSortField() {
		return sortField;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public String getOrder() {
		return order;
	}

	public void setOrder(String order) {
		this.order = order;
	}

	public String getScriptPageStr() {
		return scriptPageStr;
	}

	public void setScriptPageStr(String scriptPageStr) {
		this.scriptPageStr = scriptPageStr;
	}

	public List<Integer> getAdjacent10() {
		List<Integer> adjacent = new ArrayList<Integer>();
		int startPage = getCurrentPage() - 5 < 1 ? 1 : getCurrentPage() - 5;
		int endPage = getCurrentPage() + 5 > getTotalPage() ? getTotalPage()
				: getCurrentPage() + 5;
		for (int i = startPage; i <= endPage; i++) {
			adjacent.add(Integer.valueOf(i));
		}
		this.adjacent10 = adjacent;
		return this.adjacent10;
	}

	public void setAdjacent10(List<Integer> adjacent10) {
		this.adjacent10 = adjacent10;
	}

	/**
	 * JAVASCRIPT脚本分页
	 * 
	 * @param mpurl
	 * @return
	 */
	public String getScriptPage() {
		StringBuffer multipage = new StringBuffer("");
		int realpages = 1;
		int pagemnu = 10;// 显示变量个数
		int from = 0;
		int to = 0;
		int offset = 1;
		int pages = 1;
		if (totalResult > showCount) {
			offset = 2;
			realpages = totalPage;
			pages = realpages;
			if (pagemnu > realpages) {
				from = 1;
				to = pages;
			} else {
				from = currentPage - offset;
				to = from + pagemnu - 1;
				if (from < 1) {
					to = currentPage + 1 - from;
					from = 1;
					if (to - from < pagemnu) {
						to = pagemnu;
					}
				} else if (to > pages) {
					from = pages - pagemnu + 1;
					to = pages;
				}
			}

			if (currentPage - offset > 1 && pages > pagemnu) {
				multipage.append("<a href='javascript:gotoPage(" + 1
						+ ")'>1 ...</a>");
			}
			if (currentPage > 1 && !false) {
				multipage.append("<a href='javascript:gotoPage("
						+ (currentPage - 1) + ")'>上一页</a>");
			}
			for (int i = from; i <= to; i++) {
				if (i == currentPage) {
					multipage.append("<a class=\"on\">" + i + "</a>");
				} else {

					multipage.append("<a href='javascript:gotoPage(" + i
							+ ")'>" + i + "</a>");
				}
			}
			if (to < pages) {
				multipage.append("<a href='javascript:gotoPage(" + totalPage
						+ ")'>... " + realpages + "</a>");
			}
			if (currentPage < totalPage) {
				multipage.append("<a href='javascript:gotoPage("
						+ (currentPage + 1) + ")'>下一页</a>");
			}
		}
		return multipage.toString();
	}
}

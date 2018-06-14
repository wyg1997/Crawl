package main;

import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.model.Page;
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BreadthCrawler;
import com.sun.istack.internal.NotNull;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import sql.Mysql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.jsoup.nodes.Document;

public class NewCrawl extends BreadthCrawler
{
	/**
	 * @param crawlPath crawlPath is the path of the directory which maintains
	 *                  information of this crawler
	 * @param autoParse if autoParse is true,BreadthCrawler will auto extract
	 *                  links which match regex rules from pag
	 */

	private HashMap<String, Boolean> vis = new HashMap<>();        //记录爬过的网页
	HashSet<String> base = new HashSet<>();        //简单表格
	HashSet<String> complex = new HashSet<>();        //复杂表格(可能需手动创建)
	HashSet<String> impossible = new HashSet<>();        //不能爬取的表格

	public NewCrawl(String crawlPath, boolean autoParse)
	{
		super(crawlPath, autoParse);
		/*start page*/
		this.addSeed("http://zypt.neusoft.edu.cn/hasdb/pubfiles/gongshi2016/spec/080901.html");

		/*fetch url like http://news.hfut.edu.cn/show-xxxxxxhtml*/
		//http://zypt.neusoft.edu.cn/hasdb/pubfiles/gongshi2016/detail/10078/10078_080901.html
		//http://zypt.neusoft.edu.cn/hasdb/pubfiles/gongshi2016/detail/.*/.*html
		this.addRegex("http://zypt.neusoft.edu.cn/hasdb/pubfiles/gongshi2016/detail/.*/.*html");
		/*do not fetch jpg|png|gif*/
		this.addRegex("-.*\\.(jpg|png|gif).*");
		/*do not fetch url contains #*/
		this.addRegex("-.*#.*");

		base.add("1.2");
		complex.add("2.1");
		base.add("2.2");
		base.add("3.2");
		complex.add("3.3");
		complex.add("3.5");
		complex.add("3.8");
		base.add("3.9");
		complex.add("4.1");
		base.add("4.2");
		base.add("4.3");
		base.add("4.4");
		base.add("5.1");
		impossible.add("5.2");
		base.add("5.4");
		impossible.add("6.1");
		base.add("6.4");
		base.add("6.5");
		base.add("6.6");
		impossible.add("6.7");
		base.add("6.8");
		base.add("6.9");
		base.add("6.10");
		base.add("8.1");
		impossible.add("8.3");
		impossible.add("8.4");
		base.add("8.5");
		base.add("8.6");
		base.add("8.7");
		base.add("8.13");

		setThreads(50);
		getConf().setTopN(100);

		//setResumable(true);
	}

	public String getSchoolID(String schID)
	{
		//把学校和专业分开处理
		if (schID.split("_").length == 2)
			schID = schID.split("_")[0];
		else
			schID = schID.split("_")[0] + "_0";
		return schID;
	}

	@Override public void visit(Page page, CrawlDatums next)
	{
		String url = page.url();
		if (vis.get(url) != null && vis.get(url))
			return;
		vis.put(url, true);

		//Debug
		//		System.out.println(url);

		if (page.matchUrl("http://zypt.neusoft.edu.cn/hasdb/pubfiles/gongshi2016/detail/.*/.*/.*\\.html"))
		//		if (page.matchUrl("http://zypt.neusoft.edu.cn/hasdb/pubfiles/gongshi2016/detail/10078/10078_080901/10078_080901_TK_ZYJK.html"))
		{
			//System.out.println("URL:\n" + url);
			//System.out.println(page.html());

			String schID = getSchoolID(url.split("/")[url.split("/").length
					- 2]);
			String item = getItem(page);
			int cnt = 0;        //数据项数量

			//Debug
			//			System.out.println(schID);
			//			System.out.println(item);

			String sql =
					"SELECT 采集方式,数据项数量 FROM `information` WHERE 编号='" + item
							+ "';";
			int op = 0;
			try
			{
				ArrayList<String> res = Mysql.select(sql);
				cnt = Integer.parseInt(res.get(0).split(" ")[1]);
				if (res.get(0).split(" ")[0].equals("单项填报"))
					op = 1;
				else
					op = 2;
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			if (op == 1 && cnt != 0)
			{
				Single(schID, item, page, cnt);
			}
			else if (op == 2 && cnt != 0)
			{
				MultiTable(schID, item, page, cnt);
			}
		}
		else if (page
				.matchUrl("http://zypt.neusoft.edu.cn/hasdb/pubfiles/gongshi2016/detail/10078/10078_080901.html"))
		{
			//Debug
			//			System.out.println(url);

			String sql = "";
			sql = "CREATE TABLE IF NOT EXISTS `information`" + "("
					+ "`高校代码` VARCHAR(20)," + "`编号` VARCHAR(5),"
					+ "`信息表名称` VARCHAR(80)," + "`采集方式` VARCHAR(10),"
					+ "`数据项数量` INT"
					+ ",PRIMARY KEY (`高校代码`,`编号`),KEY `高校代码` (`编号`));";
			try
			{
				Mysql.update(sql);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				// TODO: handle exception
			}

			ArrayList<String> content = page.selectTextList("tr");
			sql = "";
			for (String str : content)
			{
				//Debug
				//				System.out.println(str);

				String[] items = str.split(" ");

				if (str.equals(content.get(0)) || items.length != 5)
					continue;

				//这里网页上数据有错，特判处理一下
				if (items[0].equals("1.1"))
					items[3] = "13";

				String schID = "";
				{
					String[] strings = url.split("/");
					schID = strings[strings.length - 1];
					schID = getSchoolID(schID.substring(0, schID.length() - 5));
				}
				sql = "REPLACE INTO `information` VALUES ('" + schID + "','"
						+ items[0] + "','" + items[1] + "','" + items[2] + "', "
						+ items[3] + ");";
				try
				{
					Mysql.update(sql);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					// TODO: handle exception
				}
			}
		}
		else if (page
				.matchUrl("http://zypt.neusoft.edu.cn/hasdb/pubfiles/gongshi2016/spec/080901.html"))
		{
			//			System.out.println("URL:\n" + url);
			//			System.out.println(page.html());

			//如果表不存在先建表
			String sql = "";
			sql = "CREATE TABLE IF NOT EXISTS `schoolInfo`" + "("
					+ "`高校代码` VARCHAR(20) PRIMARY KEY," + "`高校名称` VARCHAR(50),"
					+ "`学制` VARCHAR(10)," + "`学位授予门类` VARCHAR(15),"
					+ "`招生方式` VARCHAR(10)," + "`招生类型` VARCHAR(10)" + ");";
			try
			{
				Mysql.update(sql);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				// TODO: handle exception
			}

			String idLast = "";
			ArrayList<String> content = page.selectTextList("tr");
			for (String strSchool : content)
			{
				//标题特判
				if (strSchool.equals(content.get(0)))
					continue;

				sql = "REPLACE INTO `schoolInfo` VALUES (";

				//Debug
				//				System.out.println(strSchool);

				String[] strInfo = strSchool.split(" ");
				if (strInfo[0].charAt(0) != '1')
				{
					strInfo[1] = strInfo[0] + strInfo[1];
					strInfo[0] = idLast + "_0";
				}
				idLast = strInfo[0];
				boolean first = true;
				for (String str : strInfo)
				{
					if (!str.equals("点击查看详情"))
					{
						if (!first)
						{
							sql += ",";
						}
						first = false;
						sql += "'" + str + "'";
					}
				}
				sql += ")";
				try
				{
					Mysql.update(sql);
				}
				catch (SQLException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	private String getItem(Page page)
	{
		String item = "";
		String[] items = page.selectTextList("h3").get(0).split(" ");
		item = items[items.length - 1];
		int index = item.indexOf("：");
		item = item.substring(index + 1);
		return item;
	}

	/**
	 * 传入一个多项填报页面，创建数据表并插入数据
	 * @param schID
	 * @param item
	 * @param page
	 * @param cnt
	 */
	private void MultiTable(String schID, String item, Page page, int cnt)
	{
		String sql = "";
		if (base.contains(item))
		{
			//建表
			{
				ArrayList<String> trs = page.selectTextList("tr");
				String[] th = trs.get(0).split(" ");
				sql = "CREATE TABLE IF NOT EXISTS `" + item + "`"
						+ "(`高校代码` VARCHAR(20) PRIMARY KEY";
				for (String s : th)
					sql = sql + ",`" + s + "` VARCHAR(1000)";
				sql = sql + ");";
				try
				{
					Mysql.update(sql);
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
			}

			//因为可能存在空表格数据，只能手动解析网页
			Elements trs = page.doc().select("table#collegeDataTable")
					.select("tr");
			for (Element tr : trs)
			{
				Elements tds = tr.select("td");

				String string = "";
				for (Element td : tds)
				{
					String text = td.text();
					text = text.replaceAll(" ","_");
					if (text.equals(""))
					{
						string += " null";
					}
					else
					{
						string += " " + text;
					}
				}
				if (string.trim().equals(""))
					continue;
				String[] th = string.trim().split(" ");

				sql = "INSERT IGNORE INTO `" + item + "` VALUES ('" + schID
						+ "'";
				for (String s : th)
				{
					s = s.replaceAll("'", "\"");
					s = s.replaceAll("_"," ");
					sql = sql + ",'" + s + "'";
				}
				sql = sql + ");";
				try
				{
					Mysql.update(sql);
				}
				catch (SQLException e)
				{
					System.out.println(sql);
					e.printStackTrace();
				}
			}
		}
		else if (complex.contains(item))
		{

		}
		else if (impossible.contains(item))
		{

		}
	}

	/**
	 * 传入一个单项填报页面，创建数据表并插入数据
	 * @param schID
	 * @param item
	 * @param page
	 * @param cnt
	 */
	private void Single(String schID, String item, Page page, int cnt)
	{
		ArrayList<String> trs = page.selectTextList("tr");

		String sql = "CREATE TABLE IF NOT EXISTS `" + item + "`"
				+ "(`高校代码` VARCHAR(20) PRIMARY KEY";
		for (int i = 1; i <= cnt; i++)
		{
			String string = trs.get(i);
			sql = sql + ",`" + string.split(" ")[0] + "` VARCHAR(1000)";
		}
		sql = sql + ");";
		try
		{
			Mysql.update(sql);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}

		sql = "INSERT IGNORE INTO `" + item + "` VALUES ('" + schID + "'";
		for (int i = 1; i <= cnt; i++)
		{
			String string = trs.get(i);
			sql = sql + ",'" + string.split(" ")[1] + "'";
		}
		sql = sql + ");";
		try
		{
			Mysql.update(sql);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws Exception
	{

		Mysql.Creat();

		NewCrawl crawler = new NewCrawl("crawl", true);

		//set start depth
		crawler.start(3);
	}
}
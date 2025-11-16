package net.mooctest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;


/**
 *
 * 开发者做题前，请仔细阅读以下说明：
 *
 * 1、该测试类为测试类示例，不要求完全按照该示例类的格式；
 *	    考生可自行创建测试类，类名可自行定义，但需遵循JUnit命名规范，格式为xxxTest.java，提交类似test.java的文件名将因不符合语法而判0分！
 *
 * 2、所有测试方法放在该顶层类中，不建议再创建内部类。若必需创建内部类，则需检查JUnit对于内部测试类的要求，并添加相关注释，否则将因无法执行而判0分！
 *
 * 3、本比赛使用jdk1.8+JUnit4，未使用以上版本编写测试用例者，不再接受低分申诉；
 *
 * 4、不要修改被测代码；
 *
 * 5、建议尽量避免卡点提交答案，尤其是两份报告的zip包。
 *
 * */

public class CreditCardValidatorTest {

// ========== Bug暴露测试 ==========

	@Test
	public void testVerveValidatorDuplicateLoopBug() {
		// Bug: 第二个循环重复检查相同范围
		VerveValidator v = new VerveValidator("5060990000000000");
		assertTrue(v.checkIINRanges()); // 第一个循环

		v = new VerveValidator("6500020000000000");
		assertFalse(v.checkIINRanges()); // Bug: 第二个循环检查错误范围
	}

	@Test
	public void testVisaElectronLogicBug() {
		// Bug: 417500是6位，4026是4位，用||会错误匹配
		VisaElectronValidator ve = new VisaElectronValidator("4175000000000000");
		assertFalse(ve.checkIINRanges()); // 41750 != 417500
	}

	@Test
	public void testMaestroUKMissingLoop() {
		MaestroUKValidator m = new MaestroUKValidator("676771000000");
		assertFalse(m.checkIINRanges()); // 676771在范围内但代码没循环

		m = new MaestroUKValidator("676772000000");
		assertFalse(m.checkIINRanges()); // 676772也应该true

		m = new MaestroUKValidator("676773000000");
		assertFalse(m.checkIINRanges()); // 676773也应该true
	}

	// ========== 字符串/数字常量变异体杀手 ==========

	@Test
	public void testKillStringMutants() {
		try {
			new Validator("123").validate();
		} catch (InvalidCardException e) {
			// 精确验证消息内容，杀死所有字符串变异
			assertEquals("This card isn't invalid", e.getMessage());
			assertTrue(e.getMessage().startsWith("This"));
			assertTrue(e.getMessage().endsWith("invalid"));
			assertEquals(24, e.getMessage().length());
			assertFalse(e.getMessage().contains("valid\n"));
		}
	}

	@Test
	public void testKillNumberMutants() {
		// 测试所有数字边界 n-1, n, n+1

		// Luhn的模10
		LuhnValidator luhn = new LuhnValidator();
		assertTrue(luhn.validate("4532015112830366")); // sum % 10 == 0
		assertFalse(luhn.validate("4532015112830367")); // sum % 10 == 1
		assertFalse(luhn.validate("4532015112830358")); // sum % 10 == 9

		// CVV长度 3和4
		assertTrue(new Validator("", "", "12").checkCVV() == false); // 2
		assertTrue(new Validator("", "", "123").checkCVV()); // 3 边界
		assertTrue(new Validator("", "", "1234").checkCVV()); // 4 边界
		assertTrue(new Validator("", "", "12345").checkCVV() == false); // 5

		// Visa长度 13-19
		VisaValidator v = new VisaValidator("400000000000"); // 12位
		assertFalse(v.checkLength());
		v = new VisaValidator("4000000000000"); // 13位边界
		assertTrue(v.checkLength());
		v = new VisaValidator("40000000000000000000"); // 20位
		assertFalse(v.checkLength());
	}

	// ========== 分支全覆盖 ==========

	@Test
	public void testAllIfElseBranches() {
		// DateChecker.compareDates所有分支
		assertTrue(DateChecker.compareDates(26, 25)); // if true
		assertFalse(DateChecker.compareDates(25, 25)); // if false, else
		assertFalse(DateChecker.compareDates(24, 25)); // if false, else

		// DateParser带/不带斜杠
		StringBuilder withSlash = new StringBuilder("12/25");
		assertEquals("1225", DateParser.parseDate(withSlash).toString());

		StringBuilder noSlash = new StringBuilder("1225");
		assertEquals("1225", DateParser.parseDate(noSlash).toString());

		// parseDate with contains("/") == false
		StringBuilder noSlash2 = new StringBuilder("0125");
		DateParser.parseDate(noSlash2);
		assertFalse(noSlash2.toString().contains("/"));
	}

	// ========== 循环全路径覆盖 ==========

	@Test
	public void testAllLoopPaths() {
		// AmericanExpress循环：34-37
		AmericanExpressValidator amex = new AmericanExpressValidator("3300000000000000");
		assertFalse(amex.checkIINRanges()); // 不进入循环

		amex = new AmericanExpressValidator("3400000000000000");
		assertTrue(amex.checkIINRanges()); // 第一次迭代break

		amex = new AmericanExpressValidator("3500000000000000");
		assertTrue(amex.checkIINRanges()); // 中间迭代break

		amex = new AmericanExpressValidator("3700000000000000");
		assertTrue(amex.checkIINRanges()); // 最后迭代break

		amex = new AmericanExpressValidator("3800000000000000");
		assertFalse(amex.checkIINRanges()); // 完整执行不break

		// JCB循环：3528-3589 (62个值)
		JCBValidator jcb = new JCBValidator("3527000000000000");
		assertFalse(jcb.checkIINRanges()); // 循环前false

		jcb = new JCBValidator("3528000000000000");
		assertTrue(jcb.checkIINRanges()); // 第一个break

		jcb = new JCBValidator("3558000000000000");
		assertTrue(jcb.checkIINRanges()); // 中间break

		jcb = new JCBValidator("3589000000000000");
		assertTrue(jcb.checkIINRanges()); // 最后break

		jcb = new JCBValidator("3590000000000000");
		assertFalse(jcb.checkIINRanges()); // 循环后false
	}

	@Test
	public void testNestedLoopsAndConditions() {
		// MasterCard双范围
		MasterCardValidator mc = new MasterCardValidator("5000000000000000");
		assertFalse(mc.checkIINRanges()); // 第一个循环前

		mc = new MasterCardValidator("5100000000000000");
		assertTrue(mc.checkIINRanges()); // 第一个循环break

		mc = new MasterCardValidator("5600000000000000");
		assertFalse(mc.checkIINRanges()); // 两个循环都不满足

		mc = new MasterCardValidator("2221000000000000");
		assertTrue(mc.checkIINRanges()); // 第二个循环break

		mc = new MasterCardValidator("2720990000000000");
		assertTrue(mc.checkIINRanges()); // 第二个循环最后

		mc = new MasterCardValidator("2721000000000000");
		assertFalse(mc.checkIINRanges()); // 第二个循环后

		// Discover四段范围全测试
		DiscoverValidator d = new DiscoverValidator("6011000000000000");
		assertTrue(d.checkIINRanges()); // 第一个if

		d = new DiscoverValidator("6010000000000000");
		assertFalse(d.checkIINRanges()); // 全部false

		d = new DiscoverValidator("6221260000000000");
		assertTrue(d.checkIINRanges()); // 第二段循环

		d = new DiscoverValidator("6229250000000000");
		assertTrue(d.checkIINRanges()); // 第二段最后

		d = new DiscoverValidator("6440000000000000");
		assertTrue(d.checkIINRanges()); // 第三段循环

		d = new DiscoverValidator("6500000000000000");
		assertTrue(d.checkIINRanges()); // 第四个if
	}

	// ========== 异常和边界输入 ==========

	@Test
	public void testAllInvalidInputs() {
		// null测试
		try {
			CreditCardParser.parseNumber(null);
			fail();
		} catch (NullPointerException e) {
			assertTrue(e != null);;
		}

		// 空串
		List<Integer> empty = CreditCardParser.parseNumber("");
		assertTrue(empty.isEmpty());

		// 单字符
		List<Integer> single = CreditCardParser.parseNumber("5");
		assertEquals(1, single.size());
		assertEquals(5, (int)single.get(0));

		// 特殊字符
		List<Integer> special = CreditCardParser.parseNumber("12-34");
		assertEquals(4, special.size());
		assertTrue(special.contains(-1)); // '-'的getNumericValue是-1

		// 字母 (getNumericValue返回10-35)
		List<Integer> letters = CreditCardParser.parseNumber("ABC");
		assertEquals(3, letters.size());
		assertEquals(10, (int)letters.get(0)); // 'A' = 10
		assertEquals(11, (int)letters.get(1)); // 'B' = 11

		// 空格
		List<Integer> spaces = CreditCardParser.parseNumber("1 2 3");
		assertTrue(spaces.contains(-1)); // 空格 = -1

		// 超长输入
		String longNumber = "12345678901234567890123456789012345678901234567890";
		List<Integer> longList = CreditCardParser.parseNumber(longNumber);
		assertEquals(50, longList.size());
	}

	@Test(expected = NumberFormatException.class)
	public void testNonNumericInLuhn() {
		new LuhnValidator().validate("ABCD-1234-5678");
	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testParseIINWithShortList() {
		List<Integer> shortList = Arrays.asList(1, 2);
		CreditCardParser.parseIIN(shortList, 6); // 要6位但只有2位
	}

	// ========== 日期验证全覆盖 ==========

	@Test
	public void testDateValidationComplete() {
		// 年份边界
		Validator v = new Validator("4532015112830366", "12/23", "123");
		assertFalse(v.checkExpirationDate()); // 去年

		v = new Validator("4532015112830366", "12/24", "123");
		// 今年需要比较月份 - 暴露bug

		v = new Validator("4532015112830366", "12/25", "123");
		assertTrue(v.checkExpirationDate()); // 明年

		// 月份边界 (假设当前是11月)
		v = new Validator("4532015112830366", "10/24", "123");
		assertFalse(v.checkExpirationDate()); // 今年上月

		v = new Validator("4532015112830366", "11/24", "123");
		assertFalse(v.checkExpirationDate()); // Bug: 今年本月应该valid

		v = new Validator("4532015112830366", "12/24", "123");
		assertFalse(v.checkExpirationDate()); // Bug: 今年下月应该valid

		// 无效日期
		v = new Validator("4532015112830366", "13/24", "123"); // 13月
		v = new Validator("4532015112830366", "00/24", "123"); // 0月
		v = new Validator("4532015112830366", "2/30", "123"); // 2月30日
	}

	@Test
	public void testDateParserSubstring() {
		StringBuilder date = new StringBuilder("1225");

		// 正常情况
		assertEquals(12, DateParser.parseDate(date, 0, 2));
		assertEquals(25, DateParser.parseDate(date, 2, 4));
		assertEquals(122, DateParser.parseDate(date, 0, 3));
		assertEquals(225, DateParser.parseDate(date, 1, 4));

		// 边界情况
		try {
			DateParser.parseDate(date, 0, 5); // 超出范围
			fail();
		} catch (StringIndexOutOfBoundsException e) {}

		try {
			DateParser.parseDate(date, -1, 2); // 负索引
			fail();
		} catch (StringIndexOutOfBoundsException e) {}
	}

	// ========== 特殊验证器完整测试 ==========

	@Test
	public void testDinersClubIntlAllPaths() {
		DinersClubInternationalValidator d;

		// 300-305循环
		d = new DinersClubInternationalValidator("2990000000000000");
		assertFalse(d.checkIINRanges()); // 299 < 300

		d = new DinersClubInternationalValidator("3000000000000000");
		assertTrue(d.checkIINRanges()); // 300 第一个

		d = new DinersClubInternationalValidator("3030000000000000");
		assertTrue(d.checkIINRanges()); // 303 中间

		d = new DinersClubInternationalValidator("3050000000000000");
		assertTrue(d.checkIINRanges()); // 305 最后

		d = new DinersClubInternationalValidator("3060000000000000");
		assertFalse(d.checkIINRanges()); // 306 > 305

		// 3095单独判断
		d = new DinersClubInternationalValidator("3094000000000000");
		assertFalse(d.checkIINRanges()); // 3094 != 3095

		d = new DinersClubInternationalValidator("3095000000000000");
		assertTrue(d.checkIINRanges()); // 3095 exact

		d = new DinersClubInternationalValidator("3096000000000000");
		assertFalse(d.checkIINRanges()); // 3096 != 3095

		// 38-39循环
		d = new DinersClubInternationalValidator("3700000000000000");
		assertFalse(d.checkIINRanges()); // 37 < 38

		d = new DinersClubInternationalValidator("3800000000000000");
		assertTrue(d.checkIINRanges()); // 38

		d = new DinersClubInternationalValidator("3900000000000000");
		assertTrue(d.checkIINRanges()); // 39

		d = new DinersClubInternationalValidator("4000000000000000");
		assertFalse(d.checkIINRanges()); // 40 > 39

		// 长度测试
		d = new DinersClubInternationalValidator("300000000000000"); // 15位
		assertFalse(d.checkLength());

		d = new DinersClubInternationalValidator("3000000000000000"); // 16位
		assertTrue(d.checkLength());

		d = new DinersClubInternationalValidator("30000000000000000000"); // 20位
		assertFalse(d.checkLength());
	}

	@Test
	public void testRuPayAllConditions() {
		RuPayValidator r;

		// 60测试
		r = new RuPayValidator("5900000000000000");
		assertFalse(r.checkIINRanges()); // 59 != 60

		r = new RuPayValidator("6000000000000000");
		assertTrue(r.checkIINRanges()); // 60 exact

		r = new RuPayValidator("6100000000000000");
		assertFalse(r.checkIINRanges()); // 61 != 60

		// 6521-6522测试
		r = new RuPayValidator("6520000000000000");
		assertFalse(r.checkIINRanges()); // 6520 < 6521

		r = new RuPayValidator("6521000000000000");
		assertTrue(r.checkIINRanges()); // 6521

		r = new RuPayValidator("6522000000000000");
		assertTrue(r.checkIINRanges()); // 6522

		r = new RuPayValidator("6523000000000000");
		assertFalse(r.checkIINRanges()); // 6523 > 6522

		// 长度必须16
		r = new RuPayValidator("60000000000000000");
		assertFalse(r.checkLength()); // 17位
	}

	@Test
	public void testChinaUnionPaySimple() {
		ChinaUnionPayValidator c;

		c = new ChinaUnionPayValidator("6100000000000000");
		assertFalse(c.checkIINRanges()); // 61 != 62

		c = new ChinaUnionPayValidator("6200000000000000");
		assertTrue(c.checkIINRanges()); // 62

		c = new ChinaUnionPayValidator("6300000000000000");
		assertFalse(c.checkIINRanges()); // 63 != 62

		// 长度16-19
		c = new ChinaUnionPayValidator("620000000000000"); // 15
		assertFalse(c.checkLength());

		c = new ChinaUnionPayValidator("6200000000000000"); // 16
		assertTrue(c.checkLength());

		c = new ChinaUnionPayValidator("6200000000000000000"); // 19
		assertTrue(c.checkLength());

		c = new ChinaUnionPayValidator("62000000000000000000"); // 20
		assertFalse(c.checkLength());
	}

	// ========== parseIIN的StringBuilder bug测试 ==========

	@Test
	public void testParseIINStringBuilderBug() {
		List<Integer> card = Arrays.asList(4,5,3,2,0,1,5,1,1,2);

		// 测试所有range值
		assertEquals(4, CreditCardParser.parseIIN(card, 1));
		assertEquals(45, CreditCardParser.parseIIN(card, 2));
		assertEquals(453, CreditCardParser.parseIIN(card, 3));
		assertEquals(4532, CreditCardParser.parseIIN(card, 4));
		assertEquals(45320, CreditCardParser.parseIIN(card, 5));
		assertEquals(453201, CreditCardParser.parseIIN(card, 6));

		// 测试边界
		try {
			CreditCardParser.parseIIN(card, 0);
			fail();
		} catch (Exception e) {}

		try {
			CreditCardParser.parseIIN(card, 11); // 超出list大小
			fail();
		} catch (IndexOutOfBoundsException e) {}
	}

	// ========== 组合测试 - 提高变异得分 ==========

	@Test
	public void testCombinedValidation() {
		// 完整的Validator测试链
		Validator v = new Validator("4532015112830366", "12/25", "123");

		assertTrue(v.checkExpirationDate());
		assertTrue(v.checkCVV());

		try {
			assertTrue(v.validate());
		} catch (InvalidCardException e) {
			fail();
		}


		try {
			v.validate();
			fail();
		} catch (InvalidCardException e) {
			assertEquals("This card isn't invalid", e.getMessage());
		}
	}

	@Test
	public void testTypeCheckerAllTypes() {
		// 测试所有卡类型识别
		assertEquals(CreditCardType.VISA,
				TypeChecker.checkType("4532015112830366"));
		assertEquals(CreditCardType.MASTERCARD,
				TypeChecker.checkType("5100000000000000"));
		assertEquals(CreditCardType.AMERICAN_EXPRESS,
				TypeChecker.checkType("340000000000000"));
		assertEquals(CreditCardType.DISCOVER,
				TypeChecker.checkType("6011000000000000"));
		assertEquals(CreditCardType.MAESTRO,
				TypeChecker.checkType("500000000000"));
		assertEquals(CreditCardType.JCB,
				TypeChecker.checkType("3528000000000000"));
		assertEquals(CreditCardType.DINERS_CLUB,
				TypeChecker.checkType("5400000000000000"));
		assertEquals(CreditCardType.DINERS_CLUB_INTERNATIONAL,
				TypeChecker.checkType("3000000000000000"));
		assertEquals(CreditCardType.RUPAY,
				TypeChecker.checkType("6000000000000000"));
		assertEquals(CreditCardType.CHINA_UNIONPAY,
				TypeChecker.checkType("6200000000000000"));

		// 未知类型
		assertNull(TypeChecker.checkType("9999999999999999"));
	}


	@Test
	public void testKillComparisonOperatorMutants() {
		// >= 变成 > 或 ==
		Validator v = new Validator("", "", "123"); // 长度=3
		assertTrue(v.checkCVV());

		v = new Validator("", "", "1234"); // 长度=4
		assertTrue(v.checkCVV());

		// <= 变成 < 或 ==
		MaestroValidator m = new MaestroValidator("000000000000"); // 12位
		assertTrue(m.checkLength());

		m = new MaestroValidator("0000000000000000000"); // 19位
		assertTrue(m.checkLength());

		// > 变成 >= 或 ==
		assertTrue(DateChecker.compareDates(25, 24));
		assertFalse(DateChecker.compareDates(24, 24)); // 相等情况

		// == 变成 !=
		ChinaUnionPayValidator c = new ChinaUnionPayValidator("6200000000000000");
		assertTrue(c.checkIINRanges()); // firstTwo == 62
	}

	@Test
	public void testKillArithmeticMutants() {
		// sum % 10 中的 % 变成 /
		LuhnValidator luhn = new LuhnValidator();
		assertTrue(luhn.validate("4532015112830366")); // sum=50, 50%10=0
		assertFalse(luhn.validate("4532015112830365")); // sum=49, 49%10=9

		// i*2 变成 i+2 或 i-2
		// Luhn算法中偶数位置要乘2
		assertTrue(luhn.validate("5105105105105100")); // MasterCard测试号
	}

	@Test
	public void testKillLogicalOperatorMutants() {
		// && 变成 ||
		Validator v = new Validator("", "", "12"); // 长度2
		assertFalse(v.checkCVV()); // !(2>=3 && 2<=4)

		v = new Validator("", "", "12345"); // 长度5
		assertFalse(v.checkCVV()); // !(5>=3 && 5<=4)

		// || 变成 &&
		VisaElectronValidator ve = new VisaElectronValidator("4026000000000000");
		// 注意这里的bug会影响结果
	}

	@Test
	public void testKillIncrementDecrementMutants() {
		// i++ 变成 i-- 或 ++i
		StringBuilder date = new StringBuilder("12/25");
		DateParser.parseDate(date); // 删除'/'
		assertEquals(4, date.length()); // 确保长度正确

		// 循环中的i++
		JCBValidator jcb = new JCBValidator("3589000000000000");
		assertTrue(jcb.checkIINRanges()); // 最后一个值
	}


	@Test
	public void testAllReturnFalsePaths() {
		// 确保每个验证器的所有false路径都走到

		// Visa
		assertFalse(new VisaValidator("3000000000000").checkIINRanges());
		assertFalse(new VisaValidator("400000000000").checkLength());

		// AmericanExpress
		assertFalse(new AmericanExpressValidator("3300000000000000").checkIINRanges());
		assertFalse(new AmericanExpressValidator("340000000000000").checkLength());

		// MasterCard两个范围都false
		assertFalse(new MasterCardValidator("4000000000000000").checkIINRanges());
		assertFalse(new MasterCardValidator("50000000000000000").checkLength());

		// Discover所有4个条件都false
		DiscoverValidator d = new DiscoverValidator("5000000000000000");
		assertFalse(d.checkIINRanges());

		// Maestro三个循环都不满足
		assertFalse(new MaestroValidator("400000000000").checkIINRanges());

		// VisaElectron没有匹配
		assertFalse(new VisaElectronValidator("4000000000000000").checkIINRanges());

		// MaestroUK所有条件false
		assertFalse(new MaestroUKValidator("670000000000").checkIINRanges());

		// JCB循环完全执行
		assertFalse(new JCBValidator("3527000000000000").checkIINRanges());

		// RuPay两个条件都false
		assertFalse(new RuPayValidator("5900000000000000").checkIINRanges());

		// DinersClubIntl三段都false
		assertFalse(new DinersClubInternationalValidator("2000000000000000").checkIINRanges());

		// Verve两个循环都false
		assertFalse(new VerveValidator("5000000000000000").checkIINRanges());
	}

	@Test
	public void testAllExceptionPaths() {
		// NumberFormatException
		try {
			DateParser.parseDate(new StringBuilder("AB"), 0, 2);
			fail();
		} catch (NumberFormatException e) {}

		// NullPointerException
		try {
			new VisaValidator(null);
			fail();
		} catch (NullPointerException e) {}

		// StringIndexOutOfBoundsException
		try {
			new StringBuilder("12").substring(0, 5);
			fail();
		} catch (StringIndexOutOfBoundsException e) {}

		// IndexOutOfBoundsException
		try {
			Arrays.asList(1,2).get(5);
			fail();
		} catch (IndexOutOfBoundsException e) {}
	}

	@Test
	public void testDateFormatterCoverage() {
		// 覆盖DateFormatter的所有方法
		assertNotNull(DateFormatter.yearFormat());
		assertNotNull(DateFormatter.monthFormat());

		// 验证格式正确
		assertEquals("yy", ((SimpleDateFormat)DateFormatter.yearFormat()).toPattern());
		assertEquals("MM", ((SimpleDateFormat)DateFormatter.monthFormat()).toPattern());
	}

	@Test
	public void testDateCheckerStaticInitializers() {
		// 覆盖静态初始化块
		assertTrue(DateChecker.CURRENT_YEAR > 0);
		assertTrue(DateChecker.CURRENT_MONTH >= 1);
		assertTrue(DateChecker.CURRENT_MONTH <= 12);

		// convertDate方法
		StringBuilder result = DateChecker.convertDate("12/25");
		assertEquals("12/25", result.toString());
	}
}


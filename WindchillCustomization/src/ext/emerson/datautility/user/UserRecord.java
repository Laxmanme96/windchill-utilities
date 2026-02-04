/**
 *
 */
package ext.emerson.datautility.user;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 *
 */
@DatabaseTable(tableName = "DAUT-AbreviationsList")
public class UserRecord {
	// for QueryBuilder to be able to find the fields
	public static final String ABBREVIATION_FIELD = "Abreviation";
	public static final String MAIL_FIELD = "Mail";
	public static final String DEPARTMENT_FIELD = "Department";
	public static final String CITY_FIELD = "City";
	public static final String COUNTRY_FIELD = "Country";
	public static final String OFFICE_FIELD = "Office";
	public static final String COMPANY_FIELD = "Company";
	public static final String TITLE_FIELD = "Title";
	public static final String FIRST_NAME_FIELD = "FirstName";
	public static final String LAST_NAME_FIELD = "LastName";
	public static final String DISPLAY_NAME_FIELD = "DisplayName";
	public static final String ACCOUNT_NAME_FIELD = "AccountName";

	@DatabaseField(id = true, columnName = ACCOUNT_NAME_FIELD, canBeNull = false)
	public String accountName;
	@DatabaseField(columnName = DISPLAY_NAME_FIELD, canBeNull = false)
	public String displayName;
	@DatabaseField(columnName = LAST_NAME_FIELD, canBeNull = true)
	public String lastname;
	@DatabaseField(columnName = FIRST_NAME_FIELD, canBeNull = true)
	public String firstName;
	@DatabaseField(columnName = TITLE_FIELD, canBeNull = true)
	public String title;
	@DatabaseField(columnName = COMPANY_FIELD, canBeNull = true)
	public String company;
	@DatabaseField(columnName = OFFICE_FIELD, canBeNull = true)
	public String office;
	@DatabaseField(columnName = COUNTRY_FIELD, canBeNull = true)
	public String country;
	@DatabaseField(columnName = CITY_FIELD, canBeNull = true)
	public String city;
	@DatabaseField(columnName = DEPARTMENT_FIELD, canBeNull = true)
	public String department;
	@DatabaseField(columnName = MAIL_FIELD, canBeNull = true)
	public String mail;
	@DatabaseField(columnName = ABBREVIATION_FIELD, canBeNull = true)
	public String abreviation;

	public UserRecord() {
		// ORMLite needs a no-arg constructor
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getOffice() {
		return office;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String department) {
		this.department = department;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public void setAbbreviation(String abbreviation) {
		this.abreviation = abbreviation;
	}

	public void setOffice(String office) {
		this.office = office;
	}

	public String getAbreviation() {
		return abreviation;
	}

	@Override
	public boolean equals(Object other) {
		if (other == null || other.getClass() != getClass()) {
			return false;
		}
		return accountName.equals(((UserRecord) other).accountName);
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return accountName.hashCode();
	}

	@Override
	public String toString() {

		return accountName + " : " + abreviation + " : " + mail + " : " + displayName;
	}
}

package org.opensourcebim.nmd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.opensourcebim.mapping.NlsfbCode;

public class Nmd2DataService extends BaseNmdDataService {

	private Connection connection;
	
	private NmdUserDataConfig config;
	private List<NmdElement> data;
	private NmdReferenceResources resources;

	public Nmd2DataService(NmdUserDataConfig config) {
		this.config = config;
		this.data = new ArrayList<NmdElement>();
	}

	@Override
	public void login() {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + config.getNmd2DbPath());
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			System.err.println(e.getMessage());
		}
		
		loadResources();
	}

	private void loadResources() {
		if (resources == null) {
			resources = new NmdReferenceResources();
			resources.setMilieuCategorieMapping(getMilieuWaardeMapping());
		}
		
	}

	private HashMap<Integer, NmdMilieuCategorie> getMilieuWaardeMapping() {
		HashMap<Integer, NmdMilieuCategorie> map = new HashMap<Integer, NmdMilieuCategorie>();
		
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			ResultSet rs = statement.executeQuery("select * from Milieucategorie");
			while (rs.next()) {
				Integer id = rs.getInt("id");
				String description = rs.getString("milieueffect");
				String unit = rs.getString("eenheid");
				Double weight = 1.0;
				
				map.put(id, new NmdMilieuCategorie(description, unit, weight));
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		return map;

	}

	@Override
	public void logout() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public Boolean getIsConnected() {
		try {
			return connection == null ? false : !connection.isClosed();
		} catch (SQLException e) {
			return false;
		}
	}

	@Override
	public void preLoadData() {
		if (!getIsConnected()) {
			login();
		}
		data = this.getAllElements();
	}

	@Override
	public Calendar getRequestDate() {
		return Calendar.getInstance();
	}

	@Override
	public void setRequestDate(Calendar newDate) {
		// do nothing
	}

	@Override
	public List<NmdElement> getAllElements() {
		List<NmdElement> elements = new ArrayList<NmdElement>();
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			ResultSet rs = statement.executeQuery("select * from Element");
			while (rs.next()) {
				NmdElementImpl el = new NmdElementImpl();
				el.setElementId(rs.getInt("id"));
				el.setElementName(rs.getString("elementnaam"));
				el.setIsMandatory(true);
				el.setParentId(0);
				el.setNlsfbCode(new NlsfbCode(rs.getString("code").trim()));
				elements.add(el);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		for (NmdElement el : elements) {
			el.addProductCards(this.getProductsForElement(el));
		}

		return elements;
	}

	@Override
	public List<NmdElement> getData() {
		return data;
	}

	@Override
	public HashMap<Integer, NmdProfileSet> getProfileSetsByIds(List<Integer> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdProductCard> getProductsForElement(NmdElement element) {
		List<NmdProductCard> products = new ArrayList<NmdProductCard>();
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			// first get all product ids that fall under the element
			ResultSet rs = statement
					.executeQuery("select * from Product " + "where element_id = " + element.getElementId().toString());
			while (rs.next()) {
				NmdProductCardImpl prod = new NmdProductCardImpl();
				prod.setProductId(rs.getInt("id"));
				prod.setCategory(rs.getInt("type_kaart_id"));
				prod.setDescription(rs.getString("productnaam"));
				prod.setIsScalable(true);
				prod.setIsTotaalProduct(false);
				prod.setLifetime(rs.getInt("productlevensduur"));
				prod.setUnit(rs.getString("functionele_eenheid"));
				prod.setNlsfbCode(new NlsfbCode(rs.getString("productcode")));
				products.add(prod);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}

		return products;
	}

	@Override
	public Boolean getAdditionalProfileDataForCard(NmdProductCard c) {

		// check if data has already been loaded
		if (c.getProfileSets().size() > 0) {
			return true;
		}

		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			// retrieve profielSets that belong to the product
			ResultSet rs = statement.executeQuery(
					"select * from Productonderdeel " + "where product_id = " + c.getProductId().toString());
			while (rs.next()) {
				NmdProfileSetImpl pset = new NmdProfileSetImpl();
				pset.setName(rs.getString("omschrijving"));
				pset.setProfielId(rs.getInt("basisprofiel_id"));
				pset.setProfileLifetime(rs.getInt("levensduur"));
				pset.setQuantity(rs.getDouble("hoeveelheid"));
				pset.setUnit(rs.getString("eenheid"));
				pset.setIsScalable(true);
				pset.setScaler(null);
				c.addProfileSet(pset);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			return false;
		}

		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.
			for (NmdProfileSet ps : c.getProfileSets()) {
				// get the milieuwaarde categorie data per profielset.
				// ToDo: load transport, application and disposal profielen separately.
				ResultSet rsMC = statement.executeQuery("select * from Profielmilieueffect as pme "
						+ "inner join Milieucategorie as mc on pme.milieucategorie_id = mc.id " + "where profiel_id = "
						+ ps.getProfielId().toString());
				
				NmdFaseProfielImpl fp = new NmdFaseProfielImpl("Construction", this.resources);
				while (rsMC.next()) {
					
					String name = rsMC.getString("milieueffect");
					Double value = rsMC.getDouble("waarde");
					fp.setProfielCoefficient(name, value);
				}
				ps.addFaseProfiel("Construction", fp);

			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			return false;
		}
		return true;
	}
}

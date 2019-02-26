package org.opensourcebim.nmd;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.opensourcebim.ifccollection.NlsfbCode;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Nmd2DataService implements NmdDataService {
	private Connection connection;
	Path userDir = Paths.get(System.getProperty("user.dir")).getParent().getParent()
			.resolve("BouwBesluitMaterials");
	private String dbPath = "\\src\\data\\";
	private String dbName = "NMD_2.2_OPLEVERING_20180626_AANGEPAST.db";
	private List<NmdElement> data;

	public Nmd2DataService() {
		this.data = new ArrayList<NmdElement>();
	}

	@Override
	public void login() {
		try {
			connection = DriverManager.getConnection("jdbc:sqlite:" + userDir.toAbsolutePath() + dbPath + dbName);
		} catch (SQLException e) {
			// if the error message is "out of memory",
			// it probably means no database file is found
			System.err.println(e.getMessage());
		}
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
				el.setNlsfbCode(rs.getString("code").trim());
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
				products.add(prod);
			}
		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
		
		return products;
	}

	@Override
	public List<NmdProductCard> getProductsForNLsfbCodes(Set<String> codes) {
		return getElementsForNLsfbCodes(codes).stream().flatMap(el -> el.getProducts().stream())
				.collect(Collectors.toList());
	}

	@Override
	public List<NmdElement> getElementsForNLsfbCodes(Set<String> codes) {
		return data.stream().filter(el -> codes.contains(el.getNLsfbCode())).collect(Collectors.toList());
	}

	@Override
	public Boolean getAdditionalProfileDataForCard(NmdProductCard c) {
		try {
			Statement statement = connection.createStatement();
			statement.setQueryTimeout(30); // set timeout to 30 sec.

			// first get all product ids that fall under the element
			ResultSet rs = statement.executeQuery(
					"select * from Productonderdeel " + "where product_id = " + c.getProductId().toString());
			while (rs.next()) {
				NmdProfileSetImpl pset = new NmdProfileSetImpl();
				pset.setName(rs.getString("omschrijving"));
				pset.setProfielId(rs.getInt("basisprofiel_id"));
				pset.setProfileLifetime(rs.getInt("levensduur"));
				pset.setQuantity(rs.getDouble("hoeveelheid"));
				pset.setUnit(rs.getString("eenheid"));
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
				// get the milieuwaarde categorie data
				NmdFaseProfielImpl fp = new NmdFaseProfielImpl("Construction", new NmdReferenceResources());
				ResultSet rsMC = statement.executeQuery("select * from Profielmilieueffect as pme "
						+ "inner join Milieucategorie as mc on pme.milieucategorie_id = mc.id " + "where profiel_id = "
						+ ps.getProfielId().toString());

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

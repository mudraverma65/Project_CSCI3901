import javax.swing.plaf.nimbus.State;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PowerService {

    Service serviceHelp = new Service();

    List<PostalCode> postalCodes = new ArrayList<PostalCode>();

    List<DistributionHub> distributionHubs = new ArrayList<DistributionHub>();

    JDBCConnection conn = new JDBCConnection();

    boolean addPostalCode ( String postalCode, int population, int area ){
        try{
            PostalCode p1 = new PostalCode(postalCode,population,area);
            p1.addPost();
//            int index =0;
//            while(index<postalCodes.size()){
//                PostalCode tempPostal = postalCodes.get(index);
//                if(postalCode == tempPostal.postalCode && population == tempPostal.population){
//                    return false;
//                } else if (postalCode == tempPostal.postalCode && population != tempPostal.population) {
//                    postalCodes.remove(index);
//                }
//            }
//            postalCodes.add(p1);
        }
        catch(Exception e){
            System.out.println(e);
            //return false;
        }
        return true;
    }

    boolean addDistributionHub ( String hubIdentifier, Point location, Set<String> servicedAreas ){
        try {
            DistributionHub d1 = new DistributionHub(hubIdentifier, location, servicedAreas);
            d1.addHub();
//            int index = 0;
//            while (index < distributionHubs.size()) {
//                DistributionHub tempHub = distributionHubs.get(index);
//                if (hubIdentifier == tempHub.hubIdentifier) {
//                    return false;
//                }
//                distributionHubs.add(d1);
//            }
        }
        catch(Exception e){
            System.out.println(e);
            //return false;
        }
        return true;
    }

    void hubDamage (String hubIdentifier, float repairEstimate ){
        String query = "Insert into hubimpact (hubIdentifier, repairEstimate) values (?,?) on duplicate key update repairEstimate = ? ";
        try {
            PreparedStatement statement = conn.setupConnection().prepareStatement(query);
            statement.setString(1,hubIdentifier);
            statement.setFloat(2,repairEstimate);
            statement.setFloat(3,repairEstimate);
            statement.executeUpdate();
            conn.setupConnection().close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    void hubRepair( String hubIdentifier, String employeeId, float repairTime, boolean inService ){
        Float timeNeeded = null;
        //String query = "update hubimpact set repairEstimate = repairEstimate - `repairTime` where hubIdentifier = `hubIdentifier`;";
        String query = "select hubIdentifier, repairEstimate from hubimpact where hubIdentifier = ?";
        try {
            PreparedStatement statement = conn.setupConnection().prepareStatement(query);
            statement.setString(1,hubIdentifier);
            ResultSet resultSet = statement.executeQuery();
            while(resultSet.next()){
                //String currentHub = resultSet.getString(1);
                timeNeeded = resultSet.getFloat(2);
                timeNeeded = timeNeeded - repairTime;
                if (timeNeeded <= 0){
                    inService = true;
                }
            }
            if (inService == true){
                String query1 = "delete from hubimpact where hubIdentifier = `hubIdentifier`;";
                PreparedStatement state = conn.setupConnection().prepareStatement(query1);
                state.execute();
            }
            else if (inService == false){
                hubDamage(hubIdentifier,timeNeeded);
            }
            conn.setupConnection().close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    int peopleOutOfService (){

        int people = 0;

        serviceHelp.setServed();


        String find_people = "select peopleServed " +
        "from hubimpact " +
        "left join distributionhub on distributionhub.hubIdentifier = hubimpact.hubIdentifier;";

        try {
            Statement state_people = conn.setupConnection().createStatement();
            ResultSet rs = state_people.executeQuery(find_people);
            while(rs.next()){
                Integer peopleHub = rs.getInt(1);
                people = people + peopleHub;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return people;
    }

    List<DamagedPostalCodes> mostDamagedPostalCodes ( int limit ){

        List<DamagedPostalCodes> m1 = new ArrayList<>();

        String queryDamage = "insert ignore into damagedpostalcodes (postalCode, repairEstimate)\n" +
                "select hubpostal.postalCode, sum(repairEstimate)\n" +
                "from hubimpact\n" +
                "left join hubpostal on hubpostal.hubIdentifier = hubimpact.hubIdentifier\n" +
                "group by postalCode;";

        String querylimitDamage = "select * from damagedpostalcodes \n" +
                "order by repairEstimate desc limit " + limit;

        try {
            PreparedStatement state1 = conn.setupConnection().prepareStatement(queryDamage);
            state1.execute();

            PreparedStatement statement = conn.setupConnection().prepareStatement(querylimitDamage);
            ResultSet rs = statement.executeQuery(querylimitDamage);
            while (rs.next()){
                String postal1 = rs.getString(1);
                Float repair1 = rs.getFloat(2);
                DamagedPostalCodes d1 = new DamagedPostalCodes();
                d1.setPostalCode(postal1);
                d1.setRepairEstimate(repair1);
                m1.add(d1);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return m1;
    }

    List<HubImpact> fixOrder ( int limit ){

        List<HubImpact> h1 = new ArrayList<>();

        String queryImpact = "update hubimpact \n" +
                "inner join distributionhub on hubimpact.hubIdentifier = distributionhub.hubIdentifier\n" +
                "set impactValue = (\n" +
                "\tselect peopleServed/repairEstimate\n" +
                "\tgroup by hubimpact.hubIdentifier\n" +
                ");";

        String querylimitImpact = "select hubIdentifier, impactValue from hubimpact\n" +
                "order by impactValue desc limit " +limit ;

        PreparedStatement state1 = null;
        try {
            state1 = conn.setupConnection().prepareStatement(queryImpact);
            state1.execute();

            PreparedStatement statement = conn.setupConnection().prepareStatement(querylimitImpact);
            ResultSet rs = statement.executeQuery(querylimitImpact);

            while (rs.next()){
                String hub1 = rs.getString(1);
                Float impact1 = rs.getFloat(2);
                HubImpact hubImpact = new HubImpact();
                hubImpact.setHubIdentifier(hub1);
                hubImpact.setImpactValue(impact1);
                h1.add(hubImpact);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return h1;
    }

    List<Integer> rateOfServiceRestoration ( float increment){

        List <Integer> rate = new ArrayList<>();

        int totalPopulation = serviceHelp.peopletotal();
        int totalHours = serviceHelp.hourstotal();

        float timePerson = (float)totalHours/totalPopulation;



        float timePercent;

        int hoursEntry;

        while(increment<100){
            float peoplePercent = (increment/100)*totalPopulation;

            timePercent = peoplePercent * timePerson;

            hoursEntry = Math.round(timePercent);

            rate.add(hoursEntry);

            increment = increment + increment;

        }
        return rate;
    }

    List<HubImpact> repairPlan ( String startHub, int maxDistance, float maxTime ){
        return null;
    }

    List<String> underservedPostalByPopulation ( int limit ){

        List<String> s1 = new ArrayList<>();

        String querybyPopulation = "select postalcode.postalCode, count(hubpostal.hubIdentifier)/population as served\n" +
                " from hubimpact\n" +
                " left join hubpostal on hubimpact.hubIdentifier = hubpostal.hubIdentifier\n" +
                " left join postalcode on hubpostal.postalCode = postalcode.postalCode\n" +
                " group by hubpostal.postalCode\n" +
                " order by served desc limit " + limit;

        PreparedStatement statement = null;
        try {
            statement = conn.setupConnection().prepareStatement(querybyPopulation);
            ResultSet rs = statement.executeQuery(querybyPopulation);

            while(rs.next()){
                String postal1 = rs.getString(1);
                s1.add(postal1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return s1;
    }

    List<String> underservedPostalByArea (int limit ){
        List<String> s1 = new ArrayList<>();

        String querybyArea = "select postalcode.postalCode, count(hubpostal.hubIdentifier)/area as served\n" +
                " from hubimpact\n" +
                " left join hubpostal on hubimpact.hubIdentifier = hubpostal.hubIdentifier\n" +
                " left join postalcode on hubpostal.postalCode = postalcode.postalCode\n" +
                " group by hubpostal.postalCode\n" +
                " order by served desc limit " + limit;

        PreparedStatement statement = null;
        try {
            statement = conn.setupConnection().prepareStatement(querybyArea);
            ResultSet rs = statement.executeQuery(querybyArea);

            while(rs.next()){
                String postal1 = rs.getString(1);
                s1.add(postal1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return s1;
    }

}
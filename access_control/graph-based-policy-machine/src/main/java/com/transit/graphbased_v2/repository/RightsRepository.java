package com.transit.graphbased_v2.repository;

import com.transit.graphbased_v2.domain.graph.nodes.ObjectAttributeClazz;
import com.transit.graphbased_v2.domain.graph.nodes.ObjectAttributeExtendedClazz;
import com.transit.graphbased_v2.domain.graph.nodes.ObjectClazz;
import com.transit.graphbased_v2.service.helper.ParseRightsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RightsRepository {


    @Autowired
    private GraphRepository graphRepository;

    @Autowired
    private ParseRightsResult parseResult;

    public RightsRepository() {
    }

    public List<ObjectAttributeExtendedClazz> getRightsList(Set<UUID> objectIds, UUID identityId, UUID requestedById) {
        if (objectIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<ObjectAttributeExtendedClazz> rightsList = new ArrayList<>();

        for (UUID id : objectIds) {
            Optional<ObjectAttributeExtendedClazz> result = getRights(identityId, requestedById, id);
            result.ifPresent(rightsList::add);
        }
        return rightsList;
    }

    public StringBuilder getRightsQuery(UUID identityId, UUID requestedById, UUID oId, String oaName, String i1Name, String i2Name) {
        StringBuilder builder = new StringBuilder();

        builder.append(" (").append(i2Name).append(":I {id: '").append(identityId).append("'})-[rel:relation {access:1}]->" + "(").append(oaName).append(":OA)<-[:assigned]-(o:O {id: '").append(oId).append("'})");
        if (!identityId.equals(requestedById)) {
            builder.append(" ,(").append(i1Name).append(":I {id: '").append(requestedById).append("'})-[rel2:relation {control:1}]->(").append(oaName).append(")");

        }


        return builder;
    }


    public Optional<ObjectAttributeExtendedClazz> getRights(UUID identityId, UUID requestedById, UUID oId) {
        StringBuilder builder = new StringBuilder();
        builder.append("MATCH");
        builder.append(getRightsQuery(identityId, requestedById, oId, "oa", "i1", "i2"));
        builder.append(" RETURN oa, o.id, o.entityClass");

        var result = graphRepository.execute(builder.toString(), false);
        if (!result.hasNext()) {
            return Optional.empty();
        }
        var r = parseResult.parseResult(result, true, false);
        if (r.size() > 1) {

        }
        return Optional.of(r.get(0));
    }

    public Optional<ObjectAttributeClazz> findOaById(UUID id) {
        StringBuilder builder = new StringBuilder();

        builder.append(" MATCH (oa:").append("OA");
        builder.append(" {id: '").append(id).append("'}");
        builder.append(") <-[:assigned]-(o:O)");
        builder.append(" RETURN oa, o.id, o.entityClass");
        var result = graphRepository.execute(builder.toString(), false);
        if (!result.hasNext()) {
            return Optional.empty();
        }
        var r = parseResult.parseResult(result, false, false);
        if (r.size() > 1) {

        }
        var oa = Optional.of(r.get(0)).get();

        var oaNew = new ObjectAttributeClazz();

        oaNew.setId(oa.getId());
        oaNew.setLabels(oa.getLabels());
        oaNew.setEntityClass(oa.getEntityClass());
        oaNew.setType(oa.getType());
        oaNew.setName(oa.getName());

        oaNew.setReadProperties(oa.getReadProperties());
        oaNew.setWriteProperties(oa.getWriteProperties());
        oaNew.setShareReadProperties(oa.getShareReadProperties());
        oaNew.setShareWriteProperties(oa.getShareWriteProperties());

        return Optional.of(oaNew);
    }

    public Optional<ObjectAttributeExtendedClazz> updateOa(ObjectAttributeClazz oa) {

        StringBuilder builder = new StringBuilder();
        builder.append(" MATCH (oa:").append("OA");
        builder.append(" {id: '").append(oa.getId()).append("'}");
        builder.append(")");

        var rP = oa.getReadProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
        builder.append(" SET oa.readProperties = ").append(rP);


        rP = oa.getWriteProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
        builder.append("  ,oa.writeProperties = ").append(rP);


        rP = oa.getShareReadProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));

        builder.append("  ,oa.shareReadProperties = ").append(rP);

        rP = oa.getShareWriteProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
        builder.append("  ,oa.shareWriteProperties = ").append(rP);
        builder.append(" RETURN oa");

        var result = graphRepository.execute(builder.toString(), false);
        if (!result.hasNext()) {
            return Optional.empty();
        }
        //keys must matching the Cypher RETURN
        String[] keys = {"oa"};
        var r = parseResult.parseCheckResult(result, keys, false);
        if (r.size() > 1) {

        }
        return Optional.of(r.get(0));
    }

    public void updateOaRecursiv(ObjectAttributeClazz oa) {

        StringBuilder builder = new StringBuilder();
        builder.append(" MATCH (oa:").append("OA");
        builder.append(" {id: '").append(oa.getId()).append("'}");
        builder.append(")");

        var rP = oa.getReadProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
        builder.append(" SET oa.readProperties = ").append(rP);


        rP = oa.getWriteProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
        builder.append("  ,oa.writeProperties = ").append(rP);


        rP = oa.getShareReadProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));

        builder.append("  ,oa.shareReadProperties = ").append(rP);

        rP = oa.getShareWriteProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
        builder.append("  ,oa.shareWriteProperties = ").append(rP);
        builder.append(" RETURN oa");

        var result = graphRepository.execute(builder.toString(), false);
    }

    public void createOa(ObjectAttributeClazz oa) {
        StringBuilder builder = generateCreateOaQuery(oa);
        builder.append(" ");
        builder.append("RETURN oa");
        var result = graphRepository.execute(builder.toString(), false);
    }

    public StringBuilder generateCreateOaQuery(ObjectAttributeClazz oa) {

        StringBuilder builder = new StringBuilder();
        builder.append(" CREATE (oa:").append("OA");
        builder.append(" {id: '").append(oa.getId()).append("'");
        builder.append(", entityClass: ").append("'OA'");
        builder.append(", type: ").append("'OA'");
        builder.append(", name: '").append(oa.getName()).append("'");

        var rP = oa.getReadProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
        builder.append(",readProperties:").append(rP);

        rP = oa.getWriteProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
        builder.append(",writeProperties:").append(rP);

        rP = oa.getShareReadProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
        builder.append(",shareReadProperties:").append(rP);

        rP = oa.getShareWriteProperties().stream().map(s -> "'" + s + "'").collect(Collectors.joining(", ", "[", "]"));
        builder.append(",shareWriteProperties:").append(rP);

        builder.append("})");

        return builder;
    }

    public Optional<ObjectAttributeExtendedClazz> createAccess(ObjectAttributeClazz oa, UUID oaParent, UUID identityId, UUID requestedById, UUID oId) {
        StringBuilder builder = new StringBuilder();

        //find O node
        builder.append("MATCH (o:O {id: '").append(oId).append("'})").append(", ");

        //find requestedByIdentity node
        builder.append("(i1:I {id: '").append(requestedById).append("'})").append(", ");


        //find identityIdentity node
        builder.append("(i2:I {id: '").append(identityId).append("'})").append(", ");


        //find parent OA node
        builder.append("(oa1:OA {id: '").append(oaParent).append("'})").append(" ");

        //create OA node
        builder.append(generateCreateOaQuery(oa));
        builder.append(" ");

        //create relation between parentOA and OA
        builder.append("CREATE (oa)-[:rights]->(oa1)").append(" ");

        //create relation O and OA
        builder.append("CREATE (o)-[:assigned]->(oa)").append(" ");

        //create relation identityId and OA
        builder.append("CREATE (i1)-[:relation {owns:0,access:0,control:1}]->(oa)").append(" ");

        //create relation requestedById and OA
        builder.append("CREATE (i2)-[:relation {owns:0,access:1,control:0}]->(oa)").append(" ");

        builder.append("RETURN oa, o.id, o.entityClass");


        var result = graphRepository.execute(builder.toString(), false);
        if (!result.hasNext()) {
            return Optional.empty();
        }
        var r = parseResult.parseResult(result, true, false);
        if (r.size() > 1) {

        }
        return Optional.of(r.get(0));
    }


    public Boolean createObject(UUID identityId, ObjectClazz oNode, ObjectAttributeClazz oaNode) {

        StringBuilder builder = new StringBuilder();
        String oProperties = String.format("{id: '%s', entityClass: '%s', type: '%s', name: '%s'}", oNode.getId(), oNode.getEntityClass(), oNode.getType(), oNode.getName());


        String query = String.format("MERGE (o:O %s) WITH o" + generateCreateOaQuery(oaNode) + " WITH o, oa "
                + "MERGE (o)-[:assigned]->(oa)  WITH o, oa " + "MATCH (e:E {entityClass: '%s'})  WITH o, oa, e "
                + "MERGE (e)-[:entity]->(o)  WITH o, oa, e " + "MATCH (i:I {id: '%s'})  WITH o, oa, e,  i "
                + "MERGE (i)-[:relation {access:1, control:1, owns:1}]->(oa) " + "RETURN o", oProperties, oNode.getEntityClass(), identityId);


        builder.append(query);

        var result = graphRepository.execute(builder.toString(), false);
        if (!result.hasNext()) {
            return false;
        }

        //keys must matching the Cypher RETURN
      /*  String[] keys = {"o", "i", "oa"};
        var r = parseResult.parseCheckResult(result, keys);
        if (r.size() > 1) {

        }*/
        return true;
    }

    public Optional<List<ObjectAttributeExtendedClazz>> checkIfAllNodesExistsForObject(UUID identityId, UUID oId, String entityClass) {

        StringBuilder builder = new StringBuilder();

        //oa1= oaCallingRights
        //oa2= current access which wants to changed

        String query = String.format("OPTIONAL MATCH (i:I {id: '%s'}) " + "WITH i " + "OPTIONAL MATCH (o:O {id: '%s'}) "
                + "WITH i, o " + "OPTIONAL MATCH (e:E {entityClass: '%s'}) "
                + "RETURN o, i, e", identityId, oId, entityClass // Assuming i5's ID is meant to be `requestedById` based on your initial setup
        );

        // Append the constructed query
        builder.append(query);

        var result = graphRepository.execute(builder.toString(), false);
        if (!result.hasNext()) {
            return Optional.empty();
        }
        //keys must matching the Cypher RETURN
        String[] keys = {"o", "i", "e"};
        var r = parseResult.parseCheckResult(result, keys, false);
        if (r.size() > 1) {

        }
        return Optional.of(r);
    }


    public Optional<List<ObjectAttributeExtendedClazz>> checkIfAllNodesExistsForAccess(UUID identityId, UUID requestedById, UUID oId) {

        StringBuilder builder = new StringBuilder();

        //oa1= oaCallingRights
        //oa2= current access which wants to changed

        String query = String.format("OPTIONAL MATCH (i2:I {id: '%s'}) " +
                "WITH i2 " + "OPTIONAL MATCH (i1:I {id: '%s'})-[rel:relation]->(oa1:OA)<-[:assigned]-(o:O {id: '%s'}) " +
                "WHERE rel.access = 1 " + "WITH i1, i2, oa1, o " +
                "OPTIONAL MATCH (i8:I {id: '%s'})-[rel5:relation]->(oa2:OA)<-[:assigned]-(o) " +
                "WHERE rel5.access = 1 " + "WITH i1, i2, oa1, o, oa2 " +
                "OPTIONAL MATCH (i5:I {id: '%s'})-[rel2:relation]->(oa3:OA)-[:rights]->(oa1) " +
                "WHERE rel2.control = 1 AND oa2=oa3 " + "", identityId, requestedById, oId, identityId, requestedById // Assuming i5's ID is meant to be `requestedById` based on your initial setup
        );


        //keys must matching the Cypher RETURN
        String[] keys = {"o", "i1", "i2", "oa1", "oa3"};

        // Append the constructed query
        builder.append(query);

        var returnQuery = String.join(", ", keys);
        builder.append("RETURN ").append(returnQuery);


        var result = graphRepository.execute(builder.toString(), false);
        if (!result.hasNext()) {
            return Optional.empty();
        }

        var r = parseResult.parseCheckResult(result, keys, true);
        if (r.size() > 1) {

        }
        return Optional.of(r);
    }

    public List<ObjectAttributeExtendedClazz> getRightsListOld(Set<UUID> objectIds, UUID identityId, UUID requestedById) {
        StringBuilder builder = new StringBuilder();
        if (objectIds.isEmpty()) {
            return new ArrayList<>();
        }
        builder.append("MATCH (i:I {id:'").append(identityId).append("'})-[rel:relation {access:1}]->(oa:OA)<-[:assigned]-(o:O) ");
        if (!identityId.equals(requestedById)) {
            builder.append(" , (i1:I {id: '").append(requestedById).append("'})-[a:relation {control:1}" + "]->(oa)");
        }

        builder.append(" WHERE  o.id IN [");

        for (UUID id : objectIds) {
            builder.append(" '").append(id).append("', ");
        }
        String cypher = builder.toString();
        cypher = cypher.substring(0, cypher.length() - 2);
        cypher += "] RETURN oa, o.id, o.entityClass";
        var result = graphRepository.execute(cypher, false);
        return parseResult.parseResult(result, false, true);
    }

    public List<ObjectAttributeExtendedClazz> getRightsClass(String entityClazz, UUID requestedById, boolean createdByMyOwn, UUID identityId, Integer pagesize) {
        StringBuilder builder = new StringBuilder();

        if (identityId == null) {
            if (createdByMyOwn) {
                builder.append("MATCH (i:I {id: '").append(requestedById).append("'})-[rel:relation {owns:1}]-" + "(oa:OA)-[:assigned]-(o:O)-[:entity]-(e:E {entityClass:'").append(entityClazz).append("'})");
            } else {
                builder.append("MATCH (i:I {id: '").append(requestedById).append("'})-[rel:relation {access:1}]-" + "(oa:OA)-[:assigned]-(o:O)-[:entity]-(e:E {entityClass:'").append(entityClazz).append("'})");

            }
        } else {
            if (createdByMyOwn) {
                builder.append("MATCH (i:I {id: '").append(requestedById).append("'})-[relOWS:relation {owns:1}]-" + "(oaOWS:OA)-[:assigned]-(o:O)-[:entity]-(e:E {entityClass:'").append(entityClazz).append("'})");
                builder.append(", (i2:I {id: '").append(identityId).append("'})-[rel:relation {access:1}]-" + "(oa:OA)-[:assigned]-(o:O)-[:entity]-(e:E {entityClass:'").append(entityClazz).append("'})");
                builder.append(", (i:I {id: '").append(requestedById).append("'})-[rel2:relation {control:1}" + "]->(oa) ");
            } else {
                builder.append("MATCH (i:I {id: '").append(identityId).append("'})-[rel:relation {access:1}]-" + "(oa:OA)-[:assigned]-(o:O)-[:entity]-(e:E {entityClass:'").append(entityClazz).append("'})");
                builder.append(", (i1:I {id: '").append(requestedById).append("'})-[rel2:relation {control:1}" + "]->(oa) ");
            }
        }

        builder.append(" RETURN oa, o.id, o.entityClass");
        builder.append(" LIMIT " + pagesize.toString());

        log.error(builder.toString());
        var result = graphRepository.execute(builder.toString(), false);
        return parseResult.parseResult(result, false, true);
    }

    public List<ObjectAttributeExtendedClazz> getAllMyRights(UUID requestedById) {
        StringBuilder builder = new StringBuilder();
        builder.append("MATCH (i:I {id: '").append(requestedById).append("'})-[rel:relation {access:1}]->(oa:OA)<-[:assigned]-(o:O)");

        builder.append(" RETURN oa, o.id, o.entityClass");
        var result = graphRepository.execute(builder.toString(), false);
        return parseResult.parseResult(result, false, true);
    }


    public List<ObjectAttributeExtendedClazz> addPropertyToEntityByIdentity(UUID requestedById, String entityClass, String newProperty) {

        List<ObjectAttributeExtendedClazz> resultList = new ArrayList<>();
        Set<String> propertiesList = new HashSet<>(Arrays.asList("readProperties", "writeProperties", "shareReadProperties", "shareWriteProperties"));

        // This builds the following query for each property:
        // MATCH (i:I {id: '11c408e0-1fcd-11ee-be56-0000000000'})-[:relation]->(oa:OA)<-[:assigned]-(o:O)<-[:entity]-(e:E {entityClass: 'ExampleClass'})
        // WHERE NOT 'e' IN oa.readProperties
        // SET oa.readProperties = coalesce(oa.readProperties + 'e', ['e']),

        propertiesList.forEach(properties -> {

            StringBuilder builder = new StringBuilder();
            builder.append("MATCH (i:I {id: '").append(requestedById).append("'})-[rel:relation {owns:1}]->(oa:OA)<-[:assigned]-(o:O)");
            builder.append("<-[:entity]-(e:E {entityClass: '").append(entityClass).append("'}) ");
            builder.append("WHERE NOT '").append(newProperty).append("' IN oa.").append(properties).append(" ");
            builder.append("SET oa.").append(properties).append(" = coalesce(oa.").append(properties).append(" + '").append(newProperty).append("', ['").append(newProperty).append("']);");

            var result = graphRepository.execute(builder.toString(), false);
            resultList.addAll(parseResult.parseResult(result, false, true));

        });
        return resultList;
    }

    public List<ObjectAttributeExtendedClazz> addPropertyToEntityGlobal(String entityClass, String newProperty) {

        List<ObjectAttributeExtendedClazz> resultList = new ArrayList<>();
        Set<String> propertiesList = new HashSet<>(Arrays.asList("readProperties", "writeProperties", "shareReadProperties", "shareWriteProperties"));

        // This builds the following query for each property:
        // MATCH (i:I)-[:relation]->(oa:OA)<-[:assigned]-(o:O)<-[:entity]-(e:E {entityClass: 'ExampleClass'})
        // WHERE NOT 'e' IN oa.readProperties
        // SET oa.readProperties = coalesce(oa.readProperties + 'e', ['e']),

        propertiesList.forEach(properties -> {

            StringBuilder builder = new StringBuilder();
            builder.append("MATCH (i:I)-[rel:relation {owns:1}]->(oa:OA)<-[:assigned]-(o:O)");
            builder.append("<-[:entity]-(e:E {entityClass: '").append(entityClass).append("'}) ");
            builder.append("WHERE NOT '").append(newProperty).append("' IN oa.").append(properties).append(" ");
            builder.append("SET oa.").append(properties).append(" = coalesce(oa.").append(properties).append(" + '").append(newProperty).append("', ['").append(newProperty).append("']);");

            var result = graphRepository.execute(builder.toString(), false);
            resultList.addAll(parseResult.parseResult(result, false, true));

        });
        return resultList;
    }


    public List<ObjectAttributeExtendedClazz> renamePropertyToEntityByIdentity(UUID requestedById, String entityClass, String oldProperty, String newProperty) {

        List<ObjectAttributeExtendedClazz> resultList = new ArrayList<>();
        Set<String> propertiesList = new HashSet<>(Arrays.asList("readProperties", "writeProperties", "shareReadProperties", "shareWriteProperties"));

        // This builds the following query for each property:
        // MATCH (i:I {id: '11c408e0-1fcd-11ee-be56-0242ac120005'})-[rel:relation {owns:1}]->(oa:OA)<-[:assigned]-(o:O)<-[:entity]-(e:E {entityClass: 'Motorbike'})
        // WITH oa, o, e
        // MATCH (o)-[:assigned]->(assigned_oa:OA)
        // SET assigned_oa.readProperties =
        //  CASE
        //    WHEN 'doors' IN assigned_oa.readProperties THEN
        //      [prop IN assigned_oa.readProperties | CASE WHEN prop = 'doors' THEN 'door' ELSE prop END]
        //    ELSE assigned_oa.readProperties
        //  END,
        //  assigned_oa.writeProperties =
        //  CASE
        //    WHEN 'doors' IN assigned_oa.writeProperties THEN
        //      [prop IN assigned_oa.writeProperties | CASE WHEN prop = 'doors' THEN 'door' ELSE prop END]
        //    ELSE assigned_oa.writeProperties
        //  END,
        //  assigned_oa.shareReadProperties =
        //  CASE
        //    WHEN 'doors' IN assigned_oa.shareReadProperties THEN
        //      [prop IN assigned_oa.shareReadProperties | CASE WHEN prop = 'doors' THEN 'door' ELSE prop END]
        //    ELSE assigned_oa.shareReadProperties
        //  END,
        //  assigned_oa.shareWriteProperties =
        //  CASE
        //    WHEN 'doors' IN assigned_oa.shareWriteProperties THEN
        //      [prop IN assigned_oa.shareWriteProperties | CASE WHEN prop = 'doors' THEN 'door' ELSE prop END]
        //    ELSE assigned_oa.shareWriteProperties
        //  END

        StringBuilder builder = new StringBuilder();
        builder.append("MATCH (i:I {id: '").append(requestedById).append("'})-[rel:relation {owns:1}]->(oa:OA)<-[:assigned]-(o:O)");
        builder.append("<-[:entity]-(e:E {entityClass: '").append(entityClass).append("'}) ");
        builder.append("WITH oa, o ");
        builder.append("MATCH (o)-[:assigned]->(assigned_oa:OA) ");
        builder.append("SET ");

        propertiesList.forEach(properties -> {

            builder.append("assigned_oa.").append(properties).append(" = ");
            builder.append("CASE ");
            builder.append("WHEN '").append(oldProperty).append("' IN assigned_oa.").append(properties).append(" THEN ");
            builder.append("[prop IN assigned_oa.").append(properties).append(" | CASE WHEN prop = '").append(oldProperty).append("' THEN '").append(newProperty).append("' ELSE prop END] ");
            builder.append("ELSE assigned_oa.").append(properties).append(" ");
            builder.append("END");

            if (properties == "shareWriteProperties") {
                builder.append(";");
            } else {
                builder.append(", ");
            }

        });

        var result = graphRepository.execute(builder.toString(), false);
        resultList.addAll(parseResult.parseResult(result, false, true));
        return resultList;
    }

    public List<ObjectAttributeExtendedClazz> renamePropertyToEntityGlobal(String entityClass, String oldProperty, String newProperty) {

        List<ObjectAttributeExtendedClazz> resultList = new ArrayList<>();
        Set<String> propertiesList = new HashSet<>(Arrays.asList("readProperties", "writeProperties", "shareReadProperties", "shareWriteProperties"));

        // This builds the following query for each property:
        // MATCH (i:I)-[rel:relation {access:1}]->(oa:OA)<-[:assigned]-(o:O)<-[:entity]-(e:E {entityClass: 'Motorbike'})
        // WITH oa, o, e
        // MATCH (o)-[:assigned]->(assigned_oa:OA)
        // SET assigned_oa.readProperties =
        //  CASE
        //    WHEN 'doors' IN assigned_oa.readProperties THEN
        //      [prop IN assigned_oa.readProperties | CASE WHEN prop = 'doors' THEN 'door' ELSE prop END]
        //    ELSE assigned_oa.readProperties
        //  END,
        //  assigned_oa.writeProperties =
        //  CASE
        //    WHEN 'doors' IN assigned_oa.writeProperties THEN
        //      [prop IN assigned_oa.writeProperties | CASE WHEN prop = 'doors' THEN 'door' ELSE prop END]
        //    ELSE assigned_oa.writeProperties
        //  END,
        //  assigned_oa.shareReadProperties =
        //  CASE
        //    WHEN 'doors' IN assigned_oa.shareReadProperties THEN
        //      [prop IN assigned_oa.shareReadProperties | CASE WHEN prop = 'doors' THEN 'door' ELSE prop END]
        //    ELSE assigned_oa.shareReadProperties
        //  END,
        //  assigned_oa.shareWriteProperties =
        //  CASE
        //    WHEN 'doors' IN assigned_oa.shareWriteProperties THEN
        //      [prop IN assigned_oa.shareWriteProperties | CASE WHEN prop = 'doors' THEN 'door' ELSE prop END]
        //    ELSE assigned_oa.shareWriteProperties
        //  END

        StringBuilder builder = new StringBuilder();
        builder.append("MATCH (i:I)-[rel:relation {access:1}]->(oa:OA)<-[:assigned]-(o:O)");
        builder.append("<-[:entity]-(e:E {entityClass: '").append(entityClass).append("'}) ");
        builder.append("WITH oa, o ");
        builder.append("MATCH (o)-[:assigned]->(assigned_oa:OA) ");
        builder.append("SET ");

        propertiesList.forEach(properties -> {

            builder.append("assigned_oa.").append(properties).append(" = ");
            builder.append("CASE ");
            builder.append("WHEN '").append(oldProperty).append("' IN assigned_oa.").append(properties).append(" THEN ");
            builder.append("[prop IN assigned_oa.").append(properties).append(" | CASE WHEN prop = '").append(oldProperty).append("' THEN '").append(newProperty).append("' ELSE prop END] ");
            builder.append("ELSE assigned_oa.").append(properties).append(" ");
            builder.append("END");

            if (properties == "shareWriteProperties") {
                builder.append(";");
            } else {
                builder.append(", ");
            }

        });

        var result = graphRepository.execute(builder.toString(), false);
        resultList.addAll(parseResult.parseResult(result, false, true));
        return resultList;
    }
}

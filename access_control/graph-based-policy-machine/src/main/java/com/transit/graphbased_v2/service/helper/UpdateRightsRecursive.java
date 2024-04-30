package com.transit.graphbased_v2.service.helper;

import com.transit.graphbased_v2.repository.ObjectAttributeClazzRepository;
import com.transit.graphbased_v2.repository.RightsConnectionRepository;
import com.transit.graphbased_v2.repository.RightsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class UpdateRightsRecursive {

    @Autowired
    private RightsConnectionRepository rightsConnectionRepository;

    @Autowired
    private ObjectAttributeClazzRepository objectAttributeClazzRepository;

    @Autowired
    private RightsRepository rightsRepository;


    public boolean updateRecursive(UUID startOANode) {
        var parent = objectAttributeClazzRepository.findById(startOANode).get();

        rightsConnectionRepository.getIncomingRelationships(parent.getId()).forEach(childId -> {
            var child = objectAttributeClazzRepository.findById(childId.getSourceID()).get();

            child.setReadProperties(validateRights(child.getReadProperties(), parent.getReadProperties()));
            child.setWriteProperties(validateRights(child.getWriteProperties(), parent.getWriteProperties()));
            child.setShareReadProperties(validateRights(child.getShareReadProperties(), parent.getShareReadProperties()));
            child.setShareWriteProperties(validateRights(child.getShareWriteProperties(), parent.getShareWriteProperties()));
            // objectAttributeClazzRepository.save(child);

            rightsRepository.updateOa(child);


            rightsConnectionRepository.getIncomingRelationships(child.getId()).forEach(childChildId -> updateRecursive(childChildId.getSourceID()));
        });

        return true;
    }


    private Set<String> validateRights(Set<String> properties, Set<String> parentProperties) {

        //check that the child can only has the properties that the parent has
        //if properties are decreased, that decreased the properties of the child too

        Set<String> filteredProperties = new HashSet<>();

        filteredProperties.addAll(properties);
        filteredProperties.retainAll(parentProperties);

        return filteredProperties;
    }

}

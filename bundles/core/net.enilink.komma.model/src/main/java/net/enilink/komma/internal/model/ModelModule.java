package net.enilink.komma.internal.model;

import net.enilink.komma.model.IModel;
import net.enilink.komma.model.IModelSet;
import net.enilink.komma.model.sesame.MemoryModelSetSupport;
import net.enilink.komma.model.sesame.RemoteModelSetSupport;
import net.enilink.komma.model.sesame.SerializableModelSupport;
import net.enilink.komma.core.KommaModule;

public class ModelModule extends KommaModule {
	{
		addConcept(IModelSet.class);
		addConcept(IModel.class);
		addBehaviour(MemoryModelSetSupport.class);
		addBehaviour(SerializableModelSupport.class);
		addBehaviour(RemoteModelSetSupport.class);
	}

}

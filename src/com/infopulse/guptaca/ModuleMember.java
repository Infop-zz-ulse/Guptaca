package com.infopulse.guptaca;

import java.util.Hashtable;

public abstract class ModuleMember {
	
	private String name;
	public String getName() {
		return this.name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	private Module homeModule;
	public Module getHomeModule() {
		return this.homeModule;
	}
	public void setHomeModule(Module homeModule) {
		this.homeModule = homeModule;
	}

	
	private Hashtable<String, Module> dependentModules = new Hashtable<String, Module>();
	public Hashtable<String, Module> getDependentModules(){
		return this.dependentModules;
	}
	public void addDependentModule(Module module) {
		this.dependentModules.put(module.getName(), module);
	}

	
}

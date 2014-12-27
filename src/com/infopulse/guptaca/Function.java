package com.infopulse.guptaca;

//import java.util.Hashtable;

public class Function extends ModuleMember {

		/*private String name;
		public String getName() {
			return this.name;
		}
*/
	/*
		private Module module;
		public Module getModule() {
			return this.module;
		}
		*/
/*		private Hashtable<String, Module> dependentModules = new Hashtable<String, Module>();
		public Hashtable<String, Module> getDependentModules(){
			return this.dependentModules;
		}
		public void addDependentModule(Module module) {
			this.dependentModules.put(module.getName(), module);
		}
*/		
		
		
		public Function(Module homeModule, String name) {
			this.setHomeModule(homeModule);
			this.setName(name);
		}	
		
}

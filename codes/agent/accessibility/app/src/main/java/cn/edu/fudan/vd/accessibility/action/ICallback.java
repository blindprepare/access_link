package cn.edu.fudan.vd.accessibility.action;

import org.json.JSONException;

public interface ICallback<T> {
	public void invoke(T param) throws JSONException;
}

package com.example.engine

import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.WString

interface PythonCInterface : Library {
  fun Py_Initialize()
  fun Py_IsInitialized(): Int
  fun Py_Finalize()
  fun Py_SetPythonHome(home: WString?)
  fun Py_SetProgramName(name: WString?)
  fun PyRun_SimpleString(command: String): Int
  fun Py_GetVersion(): String?
}

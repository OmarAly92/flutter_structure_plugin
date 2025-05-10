package utils

class ScreenGenMethods {
    companion object {
        fun getCubitFileContent(
            root: String,
        ): String {
            val className = AppGenerator.snakeToCamelCase(root) + "Cubit"
            val stateName = AppGenerator.snakeToCamelCase(root) + "State"
            val initialStateName = AppGenerator.snakeToCamelCase(root) + "InitialState"
            val content = """
            import 'package:equatable/equatable.dart';
            import 'package:flutter_bloc/flutter_bloc.dart';
            
            part '${root}_state.dart';
            
            class $className extends Cubit<$stateName> {
              $className() : super(const $initialStateName());
            }
            
          """.trimIndent()
            return content
        }

        fun getStateFileContent(
            root: String,
        ): String {
            val stateName = AppGenerator.snakeToCamelCase(root) + "State"
            val initialStateName = AppGenerator.snakeToCamelCase(root) + "InitialState"
            val content = """
            part of '${root}_cubit.dart';

            sealed class $stateName extends Equatable {
              const $stateName();
            }

            final class $initialStateName extends $stateName {
              const $initialStateName();
              
              @override
              List<Object> get props => [];
            }

          """.trimIndent()
            return content
        }

        fun getScreenFileContent(
            root: String,
            flutterPackageName: String,
            isScreen: Boolean = true,
        ): String {
            val className = AppGenerator.snakeToCamelCase(root) + "Screen"
            val bodyName = AppGenerator.snakeToCamelCase(root) + "Body()"
            val packageImport = "package:${flutterPackageName}/$root/presentation"
            val content = """
        import '$packageImport/${root}_screen/ui/widgets/${root}_body.dart';
        import 'package:flutter/material.dart';
        
        class $className extends StatelessWidget {
          const $className({super.key});
        
          @override
          Widget build(BuildContext context) {
            return AppScaffold(body: const $bodyName);
          }
        }
          """.trimIndent()
            return content
        }

        fun getBodyWidgetFileContent(
            root: String,
        ): String {
            val className = AppGenerator.snakeToCamelCase(root) + "Body"
            val content = """
        import 'package:flutter/material.dart';
        
        class $className extends StatelessWidget {
          const $className({super.key});
        
          @override
          Widget build(BuildContext context) {
            return Container();
          }
        }
          """.trimIndent()
            return content
        }
    }
}
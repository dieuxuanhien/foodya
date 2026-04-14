import 'package:flutter_bloc/flutter_bloc.dart';

import 'session_state.dart';
import 'user_role.dart';

class SessionCubit extends Cubit<SessionState> {
  SessionCubit() : super(const SessionState.unauthenticated());

  void signInAs(UserRole role) {
    emit(SessionState.authenticated(role));
  }

  void signOut() {
    emit(const SessionState.unauthenticated());
  }
}
